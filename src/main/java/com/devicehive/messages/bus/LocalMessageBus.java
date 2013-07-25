package com.devicehive.messages.bus;

import static com.devicehive.messages.Transport.WEBSOCKET;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.notify.StatefulNotifier;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.jms.MessagePublisher;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Message;
import com.devicehive.model.User;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;

@Stateless
public class LocalMessageBus implements MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);

    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private DeviceCommandDAO deviceCommandDAO;
    @Inject
    private DeviceNotificationDAO deviceNotificationDAO;

    @Inject
    private StatefulNotifier notifier;
    @Inject
    private MessagesDataSource messagesDataSource;
    @Inject
    private MessagePublisher messagePublisher;
    @Inject
    private SessionMonitor sessionMonitor;

    public LocalMessageBus() {
    }

    @Override
    public void notify(MessageType messageType, Message message) throws IOException {
        logger.info("Sending message: " + message + " with type: " + messageType);

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:
            notifier.sendCommand((DeviceCommand) message);
            break;
        case DEVICE_TO_CLIENT_UPDATE_COMMAND:
            notifier.sendCommandUpdate((DeviceCommand) message);
            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            notifier.sendNotification((DeviceNotification) message);
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public DeferredResponse subscribe(MessageType messageType, MessageDetails details) throws HiveException {
        DeferredResponse deferred = new DeferredResponse();

        Long id = details.id();

        List<?> messages = new ArrayList<>();

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:
            messages = doCommandsSubscription(id, details);
            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            messages = doNotificationsSubscription(details);
            break;
        case DEVICE_TO_CLIENT_UPDATE_COMMAND:
            doCommandUpdatesSubscription(id, details);
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }

        if (messages.isEmpty()) {
            messagePublisher.addMessageListener(new MessageListener(deferred));
        }
        else {
            deferred.messages().addAll((Collection<? extends Message>) messages);
        }

        return deferred;
    }

    private void doCommandUpdatesSubscription(Long id, MessageDetails details) {
        if (id == null) {
            throw new HiveException("CommandId to subscribe for command-updates is null.");
        }
        messagesDataSource.addCommandUpdatesSubscription(details.session(), id);
    }

    private List<DeviceNotification> doNotificationsSubscription(MessageDetails details) {
        User user = getUser(details);
        if (user == null) {
            throw new HiveException("User to view notifications not found.");
        }

        messagesDataSource.addNotificationsSubscription(details.session(), details.ids());
        return deviceNotificationDAO.getByUserNewerThan(user, details.timestamp());
    }

    private List<DeviceCommand> doCommandsSubscription(Long id, MessageDetails details) throws HiveException {
        if (id == null) {
            throw new HiveException("DeviceId to subscribe for commands is null.");
        }

        Device device = deviceDAO.findById(id);
        if (device == null) {
            throw new HiveException("Device to subscribe for commands not found. DeviceId = " + id);
        }

        messagesDataSource.addCommandsSubscription(details.session(), id);
        return deviceCommandDAO.getNewerThan(device, details.timestamp());
    }

    private User getUser(MessageDetails details) {
        return details.transport() == WEBSOCKET ? WebsocketSession.getAuthorisedUser(sessionMonitor.getSession(details.session())) : details.user();
    }

    @Override
    public void unsubscribe(MessageType messageType, MessageDetails details) {
        logger.info("Unsubscribing from message type: " + messageType + " for ids: " + details.ids());

        Long id = details.id();

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:
            if (id == null) {
                logger.warn("DeviceId to unsubscribe from commands is null.");
                return;
            }
            messagesDataSource.removeCommandsSubscription(details.session(), id);
            break;
        case DEVICE_TO_CLIENT_UPDATE_COMMAND:
            if (id == null) {
                logger.warn("CommandId to unsubscribe from command-updates is null.");
                return;
            }
            messagesDataSource.removeCommandUpdatesSubscription(details.session(), id);
            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            messagesDataSource.removeNotificationsSubscription(details.session(), details.ids());
            break;
        case CLOSED_SESSION_DEVICE:
            messagesDataSource.removeCommandsSubscriptions(details.session());
            break;
        case CLOSED_SESSION_CLIENT:
            messagesDataSource.removeCommandUpdatesSubscriptions(details.session());
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }

    }

    /**
     * Method does for what described in {@link DeferredResponse} class.
     * @param <T> Message extends {@link Message} to return list of this type.
     * 
     * @param pollResult
     * @param timeout
     * @param type
     * @return Messages list of <T extends{@link Message}>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Message> List<T> expandDeferredResponse(DeferredResponse pollResult, long timeout, Class<T> type) {
        if (!pollResult.messages().isEmpty() || timeout == 0L) {
            return (List<T>) new ArrayList<>(pollResult.messages());
        }
        else {
            Lock lock = pollResult.pollLock();
            Condition hasMessages = pollResult.hasMessages();

            lock.lock();

            try {
                if (pollResult.messages().isEmpty()) {//do it only once
                    try {
                        hasMessages.await(timeout, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException e) {
                        logger.warn("hasMessages await error: ", e);
                    }
                }

                return (List<T>) new ArrayList<>(pollResult.messages());
            }
            finally {
                lock.unlock();
            }
        }
    }

}
