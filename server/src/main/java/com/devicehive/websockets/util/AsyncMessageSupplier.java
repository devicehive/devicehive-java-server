package com.devicehive.websockets.util;

import com.devicehive.json.GsonFactory;
import com.devicehive.util.LogExecutionTime;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


@EJB(beanInterface = AsyncMessageSupplier.class, name = AsyncMessageSupplier.NAME)
@Local
@Stateless
public class AsyncMessageSupplier {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageSupplier.class);

    public static final String NAME = "java:global/DeviceHive/AsyncMessageSupplier";

    private static final int RETRY_COUNT = 3;
    private static final int RETRY_DELAY = 10;


    @EJB
    private AsyncMessageSupplier self;


    @Asynchronous
    public void deliverMessages(Session session) {
        this.deliverMessages(session, 0);
    }

    @Asynchronous
    public void deliverMessages(Session session, int tryCount) {
        try {
            doDeliverMessages(session);
        } catch (IOException ex) {
            logger.error("Error message delivery, session id is {} ", session.getId());
            if (tryCount <= RETRY_COUNT) {
                logger.info("Retry in {} seconds", RETRY_DELAY);
                self.deliverMessages(session, tryCount + 1);
            } else {
                logger.info("No more tries");
            }
        }
    }

    @LogExecutionTime
    private void doDeliverMessages(Session session) throws IOException {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<JsonElement> queue =
                (ConcurrentLinkedQueue) session.getUserProperties().get(WebsocketSession.QUEUE);
        boolean acquired = false;
//        do {
            try {
                acquired = WebsocketSession.getQueueLock(session).tryLock();
                if (acquired) {
                    while (!queue.isEmpty()) {
                        JsonElement jsonElement = queue.peek();
                        if (jsonElement == null) {
                            queue.poll();
                            continue;
                        }
                        if (session.isOpen()) {
                            String data = GsonFactory.createGson().toJson(jsonElement);
                            session.getBasicRemote().sendText(data);
                            queue.poll();
                        } else {
                            logger.error("Session is closed. Unable to deliver message");
                            queue.clear();
                            return;
                        }
                        logger.debug("Session {}: {} messages left", session.getId(), queue.size());
                    }
                } //else {
//                    return;
//                }
            } finally {
                if (acquired) {
                    WebsocketSession.getQueueLock(session).unlock();
                }
            }
//        } while (!queue.isEmpty());
    }
}



