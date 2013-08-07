package com.devicehive.messages.handler;

import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.websocket.Session;
import java.util.concurrent.locks.Lock;

public abstract class WebsocketHandlerCreator implements HandlerCreator{

    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandlerCreator.class);

    private final Session session;
    private final AsyncMessageDeliverer deliverer;

    public WebsocketHandlerCreator(Session session, AsyncMessageDeliverer deliverer) {
        this.session = session;
        this.deliverer = deliverer;
    }

    protected abstract Lock getSessionLock(Session session);

    @Override
    public Runnable getHandler(final JsonElement message) {
        return new Runnable() {
            @Override
            public void run() {

                if (!session.isOpen()) {
                    return;
                }

                Lock lock = getSessionLock(session);
                try {
                    lock.lock();
                    logger.debug("Add messages to queue process for session " + session.getId());
                    WebsocketSession.addMessagesToQueue(session, message);
                }
                finally {
                    lock.unlock();
                    deliverer.deliverMessages(session);
                }
            }
        };
    }

}
