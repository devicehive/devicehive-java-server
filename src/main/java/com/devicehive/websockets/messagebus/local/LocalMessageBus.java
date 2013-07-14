package com.devicehive.websockets.messagebus.local;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.websockets.messagebus.ServerResponsesFactory;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandsSubscription;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.devicehive.websockets.util.WebsocketThreadPoolSingleton;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.locks.Lock;


@Singleton
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
    private WebsocketThreadPoolSingleton threadPoolSingleton;


    public LocalMessageBus() {
    }

    /**
     * Sends command to device websocket session
     *
     * @param deviceCommand
     * @return true if command was delivered
     */
    @Transactional
    public void submitCommand(DeviceCommand deviceCommand) {
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
            threadPoolSingleton.deliverMessagesAndNotify(session);
        }
    }

    /**
     * Sends command update to client websocket session
     *
     * @param deviceCommand
     * @return true if update was delivered
     */
    @Transactional
    public void submitCommandUpdate(DeviceCommand deviceCommand) {
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
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            logger.debug("Add messages to queue process for session " + session.getId());
            WebsocketSession.addMessagesToQueue(session, jsonObject);
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
            logger.debug("Deliver messages process for session " + session.getId());
            threadPoolSingleton.deliverMessagesAndNotify(session);
        }
    }

    /**
     * Subscrbes given device to commands
     *
     * @param device
     * @param session
     */
    @Transactional
    public void subscribeForCommands(Device device, Session session) {
        logger.debug("Subscribing for commands for device : " + device.getId() + " and session : " + session.getId());
        commandSubscriptionDAO.deleteByDeviceAndSession(device.getId(), session.getId());
        commandSubscriptionDAO.insert(new CommandsSubscription(device.getId(), session.getId()));
    }

    /**
     * Subscrbes given device to commands
     *
     * @param device
     * @param sessionId
     */
    @Transactional
    public void unsubscribeFromCommands(Device device, String sessionId) {
        logger.debug("Unsubscribing from commands for device : " + device.getId() + " and session : " + sessionId);
        commandSubscriptionDAO.deleteByDeviceAndSession(device.getId(), sessionId);
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
    @Transactional
    //TODO make this multithreaded ?!
    public void submitNotification(DeviceNotification deviceNotification) {
        logger.debug("Submit notification action for deviceNotification :" + deviceNotification.getId());
        JsonObject resultMessage = ServerResponsesFactory.createNotificationInsertMessage(deviceNotification);

        Set<Session> delivers = new HashSet();

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
                threadPoolSingleton.deliverMessagesAndNotify(session);

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
            notificationSubscriptionDAO.insertSubscriptions(sessionId);
        } else {
            notificationSubscriptionDAO.insertSubscriptions(devices, sessionId);
        }
    }

    /**
     * Unsubscribes client websocket session from device notifications
     *
     * @param sessionId
     * @param devices
     */
    @Transactional
    public void unsubscribeFromNotifications(String sessionId, Collection<Device> devices) {
        if (devices == null) {
            notificationSubscriptionDAO.deleteBySession(sessionId);
        } else {
            if (devices.isEmpty()) {
                return;
            }
            List<Long> list = new ArrayList<Long>(devices.size());
            for (Device device : devices) {
                list.add(device.getId());
            }
            for (Device device : devices) {
                notificationSubscriptionDAO.deleteByDeviceAndSession(device, sessionId);
            }
        }
    }

    public void onDeviceSessionClose(Session session) {
        commandSubscriptionDAO.deleteBySession(session.getId());
    }

    public void onClientSessionClose(Session session) {
        commandSubscriptionDAO.deleteBySession(session.getId());
        notificationSubscriptionDAO.deleteBySession(session.getId());
    }


}
