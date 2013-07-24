package com.devicehive.websockets.messagebus.local;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.websockets.handlers.ServerResponsesFactory;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandsSubscription;
import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.locks.Lock;

@DataSourceDefinition(
        className = Constants.DATA_SOURCE_CLASS_NAME,
        name = Constants.DATA_SOURCE_NAME,
        databaseName = "memory:devicehive;create=true",
        transactional = true,
        isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
        initialPoolSize = 2,
        minPoolSize = 2,
        maxPoolSize = 100
)
@Stateless
public class LocalMessageBus {

    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);
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
    private AsyncMessageDeliverer asyncMessageDeliverer;


    public LocalMessageBus() {
    }

    /**
     * Sends command to device websocket session
     *
     * @param deviceCommand
     * @return true if command was delivered
     */

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
        } finally {
            lock.unlock();
            logger.debug("Deliver messages process for session " + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    /**
     * Sends command update to client websocket session
     *
     * @param deviceCommand
     * @return true if update was delivered
     */

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
        } finally {
            WebsocketSession.getCommandUpdatesSubscriptionsLock(session).unlock();
            logger.debug("Deliver messages process for session " + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    /**
     * Subscrbes given device to commands
     *
     * @param device
     * @param sessionId
     */
    public void subscribeForCommands(Device device, String sessionId) {
        logger.debug("Subscribing for commands for device : " + device.getId() + " and session : " + sessionId);
        commandSubscriptionDAO.deleteByDevice(device.getId());
        commandSubscriptionDAO.insert(new CommandsSubscription(device.getId(), sessionId));
    }

    /**
     * Subscrbes given device to commands
     *
     * @param device
     * @param sessionId
     */
    public void unsubscribeFromCommands(Device device, String sessionId) {
        logger.debug("Unsubscribing from commands for device : " + device.getId() + " and session : " + sessionId);
        commandSubscriptionDAO.deleteByDevice(device.getId());
    }

    public void subscribeForCommandUpdates(Long commandId, Session session) {
        logger.debug("Subscribing for commands update for command : " + commandId + " and session : " +
                session.getId());
        commandUpdatesSubscriptionDAO.deleteByCommandId(commandId);
        commandUpdatesSubscriptionDAO.insert(new CommandUpdatesSubscription(commandId, session.getId()));
    }

    /**
     * Sends device notification to clients
     *
     * @param deviceNotification
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
        if (sessions != null) {
            delivers.addAll(sessions);
        }

        for (Session session : delivers) {
            Lock lock = WebsocketSession.getNotificationSubscriptionsLock(session);
            try {
                lock.lock();
                logger.debug("add messages to queue for session : " + session.getId());
                WebsocketSession.addMessagesToQueue(session, resultMessage);
            } finally {
                lock.unlock();
                logger.debug("deliver messages for session : " + session.getId());
                asyncMessageDeliverer.deliverMessages(session);

            }
        }
    }

    /**
     * Subscribes client websocket session to device notifications
     *
     * @param sessionId
     * @param devices
     */
    public void subscribeForNotifications(String sessionId, Collection<Device> devices) {
        if (devices == null) {
            notificationSubscriptionDAO.deleteBySession(sessionId);
            notificationSubscriptionDAO.insertSubscriptions(sessionId);
        } else {

            notificationSubscriptionDAO.deleteByDevicesAndSession(sessionId, devices);
            notificationSubscriptionDAO.insertSubscriptions(devices, sessionId);
        }
    }

    /**
     * Unsubscribes client websocket session from device notifications
     *
     * @param sessionId
     * @param devices
     */
    public void unsubscribeFromNotifications(String sessionId, Collection<Device> devices) {
        if (devices == null) {
            notificationSubscriptionDAO.deleteBySession(sessionId);
        } else {
            if (devices.isEmpty()) {
                return;
            }
            notificationSubscriptionDAO.deleteByDevicesAndSession(sessionId, devices);

        }
    }

    public void onDeviceSessionClose(String sessionId) {
        commandSubscriptionDAO.deleteBySession(sessionId);
    }

    public void onClientSessionClose(String sessionId) {
        commandUpdatesSubscriptionDAO.deleteBySession(sessionId);
        notificationSubscriptionDAO.deleteBySession(sessionId);
    }


}
