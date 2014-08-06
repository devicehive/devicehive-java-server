package com.devicehive.websockets.util;

import com.devicehive.json.GsonFactory;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.enterprise.event.Observes;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AsyncMessageSupplier {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageSupplier.class);



    @EJB
    private AsyncMessageSupplier self;


    @LogExecutionTime
    @Asynchronous
    public void deliverMessages(@Observes @FlushQueue Session session) throws IOException {
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<JsonElement> queue = HiveWebsocketSessionState.get(session).getQueue();
        boolean acquired = false;
        try {
            acquired = HiveWebsocketSessionState.get(session).getQueueLock().tryLock();
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
            }
        } finally {
            if (acquired) {
                HiveWebsocketSessionState.get(session).getQueueLock().unlock();
            }
        }
    }

}



