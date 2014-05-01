package com.devicehive.messages.handler;

import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.websocket.Session;
import java.util.concurrent.locks.Lock;

public class WebsocketHandlerCreator implements HandlerCreator {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandlerCreator.class);

    private final Session session;

    private final Lock lock;

    public WebsocketHandlerCreator(Session session, String lockAttribute) {
        this.session = session;
        this.lock = (Lock) session.getUserProperties().get(lockAttribute);
    }


    @Override
    public Runnable getHandler(final JsonElement message) {
        logger.debug("Websocket subscription notified");
        return new Runnable() {
            @Override
            public void run() {
                if (!session.isOpen()) {
                    return;
                }
                try {
                    lock.lock();
                    logger.debug("Add messages to queue process for session " + session.getId());
                    WebsocketSession.addMessagesToQueue(session, message);
                } finally {
                    lock.unlock();
                }

                try {
                    InitialContext initialContext = new InitialContext();
                    AsyncMessageSupplier supplier = (AsyncMessageSupplier) initialContext.lookup(AsyncMessageSupplier.NAME);
                    supplier.deliverMessages(session);
                } catch (NamingException e) {
                    logger.error("Can not get AsyncMessageSupplier bean", e);
                }
            }
        };
    }

}
