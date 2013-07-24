package com.devicehive.messages.bus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.messages.bus.global.GlobalMessageBus;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;
import com.devicehive.messages.jms.MessagePublisher;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Message;
import com.devicehive.model.MessageType;
import com.devicehive.model.User;
import com.devicehive.websockets.handlers.ServerResponsesFactory;
import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonObject;

@Stateless
public class LocalMessageBus implements MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);

    @Inject
    private GlobalMessageBus globalBus;
    @Inject
    private UserDAO userDAO;
    @Inject
    private NotificationSubscriptionDAO notificationSubscriptionDAO;
    @Inject
    private CommandSubscriptionDAO commandSubscriptionDAO;
    @Inject
    private SessionMonitor sessionMonitor;
    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private DeviceCommandDAO deviceCommandDAO;
    @Inject
    private DeviceNotificationDAO deviceNotificationDAO;
    @Inject
    private AsyncMessageDeliverer asyncMessageDeliverer;
    @Inject
    private MessagesDataSource messagesDataSource;
    @Inject
    private MessagePublisher messagePublisher;

    public LocalMessageBus() {
    }

    @Override
    public void send(MessageType messageType, Message message) throws IOException {
        logger.info("Sending message: " + message + " with type: " + messageType);

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:
            submitCommand((DeviceCommand) message);
            break;
        case DEVICE_TO_CLIENT_UPDATE_COMMAND:
            submitCommandUpdate((DeviceCommand) message);
            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            submitNotification((DeviceNotification) message);
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }
    }

    public void submitCommand(DeviceCommand deviceCommand) throws IOException {
        logger.debug("Getting subscription for command " + deviceCommand.getId());
        CommandsSubscription commandsSubscription = commandSubscriptionDAO.getByDeviceId(deviceCommand.getDevice()
                .getId());
        if (commandsSubscription == null) {
            return;
        }
        logger.debug("Subscription for command " + deviceCommand.getId() + ": " + commandsSubscription);
        Session session = sessionMonitor.getSession(commandsSubscription.getSessionId());
        if (session == null || !session.isOpen()) {
            return;
        }
        JsonObject jsonObject = ServerResponsesFactory.createCommandInsertMessage(deviceCommand);

        Lock lock = WebsocketSession.getCommandsSubscriptionsLock(session);
        try {
            lock.lock();
            logger.debug("Add messages to queue process for session " + session.getId());
            WebsocketSession.addMessagesToQueue(session, jsonObject);
        }
        finally {
            lock.unlock();
            logger.debug("Deliver messages process for session " + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    public void submitCommandUpdate(DeviceCommand deviceCommand) throws IOException {
        logger.debug("Submitting command update for command " + deviceCommand.getId());
        CommandUpdatesSubscription commandUpdatesSubscription =
                commandUpdatesSubscriptionDAO.getByCommandId(deviceCommand.getId());
        if (commandUpdatesSubscription == null) {
            logger.warn("No updates for command with id = " + deviceCommand.getId() + " found");
            return;
        }
        Session session = sessionMonitor.getSession(commandUpdatesSubscription.getSessionId());

        if (session == null || !session.isOpen()) {
            return;
        }
        JsonObject jsonObject = ServerResponsesFactory.createCommandUpdateMessage(deviceCommand);
        try {
            WebsocketSession.getCommandUpdatesSubscriptionsLock(session).lock();
            logger.debug("Add messages to queue process for session " + session.getId());
            WebsocketSession.addMessagesToQueue(session, jsonObject);
        }
        finally {
            WebsocketSession.getCommandUpdatesSubscriptionsLock(session).unlock();
            logger.debug("Deliver messages process for session " + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    public void submitNotification(DeviceNotification deviceNotification) throws IOException {
        logger.debug("Submit notification action for deviceNotification :" + deviceNotification.getId());
        JsonObject resultMessage = ServerResponsesFactory.createNotificationInsertMessage(deviceNotification);

        Set<Session> delivers = new HashSet<>();

        logger.debug("Getting sessionIdsSubscribedForAll");
        List<String> sessionIdsSubscribedForAll = notificationSubscriptionDAO.getSessionIdSubscribedForAll();
        logger.debug("Getting sessions subscribed for all");
        Set<Session> subscribedForAll = new HashSet<>();
        for (String sessionId : sessionIdsSubscribedForAll) {
            subscribedForAll.add(sessionMonitor.getSession(sessionId));
        }
        for (Session session : subscribedForAll) {
            User user = WebsocketSession.getAuthorisedUser(session);
            if (userDAO.hasAccessToNetwork(user, deviceNotification.getDevice().getNetwork())) {
                delivers.add(session);
            }
        }

        Long deviceId = deviceDAO.findByUUID(deviceNotification.getDevice().getGuid()).getId();
        Collection<String> sessionIds = notificationSubscriptionDAO.getSessionIdSubscribedByDevice(deviceId);

        Set<Session> sessions = new HashSet<>();
        for (String sesionId : sessionIds) {
            sessions.add(sessionMonitor.getSession(sesionId));

        }

        delivers.addAll(sessions);

        for (Session session : delivers) {
            Lock lock = WebsocketSession.getNotificationSubscriptionsLock(session);
            try {
                lock.lock();
                logger.debug("add messages to queue for session : " + session.getId());
                WebsocketSession.addMessagesToQueue(session, resultMessage);
            }
            finally {
                lock.unlock();
                logger.debug("deliver messages for session : " + session.getId());
                asyncMessageDeliverer.deliverMessages(session);
            }
        }
    }

    @Override
    public void subscribe(MessageType messageType, String sessionId, List<Long> ids) {
        logger.info("Subscribing to message type: " + messageType + " for ids: " + ids);

        Long id = ids != null && !ids.isEmpty() ? ids.get(0) : null;

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:
            if (id == null) {
                logger.warn("DeviceId to subscribe for commands is null.");
                return;
            }
            messagesDataSource.addCommandsSubscription(sessionId, id);
            break;
        case DEVICE_TO_CLIENT_UPDATE_COMMAND:
            if (id == null) {
                logger.warn("CommandId to subscribe for command-updates is null.");
                return;
            }
            messagesDataSource.addCommandUpdatesSubscription(sessionId, id);
            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            messagesDataSource.addNotificationsSubscription(sessionId, ids);
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }
    }

    @Override
    public void subscribe(MessageType messageType, String sessionId, Long... ids) {
        subscribe(messageType, sessionId, Arrays.asList(ids));
    }

    @Override
    public void unsubscribe(MessageType messageType, String sessionId, List<Long> ids) {
        logger.info("Unsubscribing from message type: " + messageType + " for ids: " + ids);

        Long id = ids != null && !ids.isEmpty() ? ids.get(0) : null;

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:
            if (id == null) {
                logger.warn("DeviceId to unsubscribe from commands is null.");
                return;
            }
            messagesDataSource.removeCommandsSubscription(sessionId, id);
            break;
        case DEVICE_TO_CLIENT_UPDATE_COMMAND:
            if (id == null) {
                logger.warn("CommandId to unsubscribe from command-updates is null.");
                return;
            }
            messagesDataSource.removeCommandUpdatesSubscription(sessionId, id);
            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            messagesDataSource.removeNotificationsSubscription(sessionId, ids);
            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }

    }

    @Override
    public void unsubscribe(MessageType messageType, String sessionId, Long... ids) {
        unsubscribe(messageType, sessionId, Arrays.asList(ids));
    }

    @SuppressWarnings("unchecked")
    @Override
    public PollResult poll(MessageType messageType, Date timestamp, Long id) {
        PollResult pollResult = new PollResult();

        if (id == null) {
            return null;
        }

        List<?> messages = new ArrayList<>();

        switch (messageType) {
        case CLIENT_TO_DEVICE_COMMAND:

            Device device = deviceDAO.findById(id);
            if (device == null) {
                return null;
            }

            messages = deviceCommandDAO.getNewerThan(device, timestamp);

            break;
        case DEVICE_TO_CLIENT_NOTIFICATION:
            User user = userDAO.findById(id);
            if (user == null) {
                return null;
            }

            messages = deviceNotificationDAO.getByUserNewerThan(user, timestamp);

            break;
        default:
            logger.warn("Unsupported MessageType found: " + messageType);
            break;
        }

        if (messages.isEmpty()) {
            messagePublisher.addMessageListener(new MessageListener(pollResult));
        }
        else {
            pollResult.messages().addAll((Collection<? extends Message>) messages);
        }

        return pollResult;

    }

    @Override
    public void unsubscribeDevice(String sessionId) {
        messagesDataSource.removeCommandsSubscriptions(sessionId);
    }

    @Override
    public void unsubscribeClient(String sessionId) {
        messagesDataSource.removeCommandUpdatesSubscriptions(sessionId);
    }

    /**
     * Method does for what described in {@link PollResult} class.
     * @param <T>
     * 
     * @param pollResult
     * @param timeout
     * @param type 
     * @return Messages
     */
    @SuppressWarnings("unchecked")
    public static <T extends Message> List<T> expandPollResult(PollResult pollResult, long timeout, Class<T> type) {
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
                        logger.warn("hasMessages await: ", e);
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
