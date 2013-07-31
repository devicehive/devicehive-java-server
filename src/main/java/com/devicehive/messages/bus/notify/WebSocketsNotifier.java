package com.devicehive.messages.bus.notify;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.messages.bus.LocalMessageBus;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.cluster.hazelcast.HazelcastBased;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Message;
import com.devicehive.model.User;
import com.devicehive.websockets.handlers.ServerResponsesFactory;
import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonObject;

/**
 * Class-notifier. It can be used to deliver messages to open websocket connections.
 * Use it if you have any {@link Message} and you want to send it to session.
 * @author rroschin
 *
 */
@Singleton
public class WebSocketsNotifier implements StatefulNotifier {

    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);

    @Inject
    private UserDAO userDAO;
    @Inject
    private DeviceDAO deviceDAO;

    @Inject
    private SessionMonitor sessionMonitor;
    @Inject
    private AsyncMessageDeliverer asyncMessageDeliverer;
    @Inject
    @HazelcastBased
    private MessagesDataSource messagesDataSource;

    @Override
    public void sendCommand(DeviceCommand deviceCommand) throws IOException {
        logger.debug("Getting subscription for command " + deviceCommand.getId());
        CommandsSubscription commandsSubscription = messagesDataSource.commandSubscriptions().getByDeviceId(deviceCommand.getDevice().getId());
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

    @Override
    public void sendCommandUpdate(DeviceCommand deviceCommand) throws IOException {
        logger.debug("Submitting command update for command " + deviceCommand.getId());
        CommandUpdatesSubscription commandUpdatesSubscription = messagesDataSource.commandUpdatesSubscriptions().getByCommandId(deviceCommand.getId());
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

    @Override
    public void sendNotification(DeviceNotification deviceNotification) throws IOException {
        logger.debug("Submit notification action for deviceNotification :" + deviceNotification.getId());
        JsonObject resultMessage = ServerResponsesFactory.createNotificationInsertMessage(deviceNotification);

        Set<Session> delivers = new HashSet<>();

        logger.debug("Getting sessionIdsSubscribedForAll");
        List<String> sessionIdsSubscribedForAll = messagesDataSource.notificationSubscriptions().getSessionIdSubscribedForAll();
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
        Collection<String> sessionIds = messagesDataSource.notificationSubscriptions().getSessionIdSubscribedByDevice(deviceId);

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

}
