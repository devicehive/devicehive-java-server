package com.devicehive.messages.handler;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.FlushQueue;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.websocket.Session;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

public abstract class WebsocketHandlerCreator<T> implements HandlerCreator<T> {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandlerCreator.class);

    private final Session session;

    private final Lock lock;

    private WebsocketHandlerCreator(Session session, Lock lock) {
        this.session = session;
        this.lock = lock;
    }


    public static WebsocketHandlerCreator<DeviceCommand> createCommandInsert(Session session) {
        return new WebsocketHandlerCreator<DeviceCommand>(session, HiveWebsocketSessionState.get(session).getCommandSubscriptionsLock()) {
            @Override
            protected JsonObject createJsonObject(DeviceCommand message, UUID subId) {
                return ServerResponsesFactory.createCommandInsertMessage(message, subId);
            }
        };
    }

    public static WebsocketHandlerCreator<DeviceCommand> createCommandUpdate(Session session) {
        return new WebsocketHandlerCreator<DeviceCommand>(session, HiveWebsocketSessionState.get(session).getCommandUpdateSubscriptionsLock()) {
            @Override
            protected JsonObject createJsonObject(DeviceCommand message, UUID subId) {
                return ServerResponsesFactory.createCommandUpdateMessage(message);
            }
        };
    }

    public static WebsocketHandlerCreator<DeviceNotification> createNotificationInsert(Session session) {
        return new WebsocketHandlerCreator<DeviceNotification>(session, HiveWebsocketSessionState.get(session).getNotificationSubscriptionsLock()) {
            @Override
            protected JsonObject createJsonObject(DeviceNotification message, UUID subId) {
                return ServerResponsesFactory.createNotificationInsertMessage(message, subId);
            }
        };
    }

    protected abstract JsonObject createJsonObject(T message, UUID subId);

    @Override
    public Runnable getHandler(final T message, final UUID subId) {
        logger.debug("Websocket subscription notified");

        return new Runnable() {
            @Override
            public void run() {
                if (!session.isOpen()) {
                    return;
                }
                JsonObject json = createJsonObject(message, subId);
                try {
                    lock.lock();
                    logger.debug("Add messages to queue process for session " + session.getId());
                    HiveWebsocketSessionState.get(session).getQueue().add(json);
                } finally {
                    lock.unlock();
                }
                CDI.current().getBeanManager().fireEvent(session, flushQueue);
            }
        };
    }


    private static AnnotationLiteral<FlushQueue> flushQueue = new AnnotationLiteral<FlushQueue>() {};
}
