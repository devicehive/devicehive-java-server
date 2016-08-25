package com.devicehive.messages.handler;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.util.ApplicationContextHolder;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.HiveWebSocketSessionState;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.locks.Lock;

public abstract class WebsocketHandlerCreator<T> implements HandlerCreator<T> {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandlerCreator.class);

    private final WebSocketSession session;
    private final Lock lock;


    private WebsocketHandlerCreator(WebSocketSession session, Lock lock) {
        this.session = session;
        this.lock = lock;
    }

    public static WebsocketHandlerCreator<DeviceCommand> createCommandInsert(WebSocketSession session) {
        return new WebsocketHandlerCreator<DeviceCommand>(session,
                HiveWebSocketSessionState.get(session)
                        .getCommandSubscriptionsLock()) {
            @Override
            protected JsonObject createJsonObject(DeviceCommand message, String subId) {
                return ServerResponsesFactory.createCommandInsertMessage(message, subId);
            }
        };
    }

    public static WebsocketHandlerCreator<DeviceCommand> createCommandUpdate(WebSocketSession session) {
        return new WebsocketHandlerCreator<DeviceCommand>(session,
                HiveWebSocketSessionState.get(session)
                        .getCommandUpdateSubscriptionsLock()) {
            @Override
            protected JsonObject createJsonObject(DeviceCommand message, String subId) {
                return ServerResponsesFactory.createCommandUpdateMessage(message);
            }
        };
    }

    public static WebsocketHandlerCreator<DeviceNotification> createNotificationInsert(WebSocketSession session) {
        return new WebsocketHandlerCreator<DeviceNotification>(session,
                HiveWebSocketSessionState.get(session)
                        .getNotificationSubscriptionsLock()) {
            @Override
            protected JsonObject createJsonObject(DeviceNotification message, String subId) {
                return ServerResponsesFactory.createNotificationInsertMessage(message, subId);
            }
        };
    }

    protected abstract JsonObject createJsonObject(T message, String subId);

    @Override
    public Runnable getHandler(final T message, final String subId) {
        logger.debug("Websocket subscription notified");

        return () -> {
            if (!session.isOpen()) {
                return;
            }
            JsonObject json = createJsonObject(message, subId);
            try {
                lock.lock();
                logger.debug("Add messages to queue process for session " + session.getId());
                HiveWebSocketSessionState.get(session).getQueue().add(json);
            } finally {
                lock.unlock();
            }
            ApplicationContextHolder.getApplicationContext().getBean(AsyncMessageSupplier.class).deliverMessages(session);
        };
    }
}
