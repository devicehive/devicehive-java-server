package com.devicehive.messages.bus;

import static com.devicehive.messages.Transport.WEBSOCKET;
import static com.devicehive.model.HiveEntity.INITIAL_ENTITY_VERSION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import com.devicehive.messages.Transport;
import com.devicehive.messages.bus.notify.StatefulNotifier;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.hash.HashMapBased;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Message;
import com.devicehive.model.User;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;

/**
 * Implementation of {@link MessageBus} with some useful methods.
 * @author rroschin
 *
 */
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
    @HashMapBased
    /* Supported implementations: DerbyBased and HashMapBased */
    private MessagesDataSource messagesDataSource;
    @Inject
    private MessageBroadcaster messagePublisher;
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
            messages = doCommandUpdatesSubscription(id, details);
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }

        if (messages.isEmpty() && details.transport() == Transport.REST) {
            messagePublisher.addMessageListener(new StatelessMessageListener(deferred));
        }
        else {
            deferred.messages().addAll((Collection<? extends Message>) messages);
        }

        return deferred;
    }

    private List<DeviceCommand> doCommandUpdatesSubscription(Long id, MessageDetails details) {
        if (id == null) {
            throw new HiveException("CommandId to subscribe for command-updates is null.");
        }
        messagesDataSource.addCommandUpdatesSubscription(details.session(), id);

        if (details.ids().size() == 2) {//deviceId and commandId
            //We need to return command only if it has been updated by device
            DeviceCommand command = deviceCommandDAO.findById(details.ids().get(1));
            boolean wasUpdate = command.getEntityVersion() > INITIAL_ENTITY_VERSION;
            return wasUpdate ? Arrays.asList(command) : Collections.<DeviceCommand> emptyList();
        }
        return Collections.<DeviceCommand> emptyList();
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
     * @param deferred
     * @param timeout
     * @param type
     * @return Messages list of <T extends {@link Message}>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Message> List<T> expandDeferredResponse(DeferredResponse deferred, long timeout, Class<T> type) {
        if (!deferred.messages().isEmpty() || timeout == 0L) {
            return (List<T>) new ArrayList<>(deferred.messages());
        }
        else {
            Lock lock = deferred.pollLock();
            Condition hasMessages = deferred.hasMessages();

            lock.lock();

            try {
                if (deferred.messages().isEmpty()) {//do it only once
                    try {
                        hasMessages.await(timeout, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException e) {
                        logger.warn("hasMessages await error: ", e);
                    }
                }

                return (List<T>) new ArrayList<>(deferred.messages());
            }
            finally {
                lock.unlock();
            }
        }
    }
}
