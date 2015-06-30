package com.devicehive.websockets.util;

import com.devicehive.json.GsonFactory;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class AsyncMessageSupplier {
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageSupplier.class);

    public void deliverMessages(WebSocketSession session) {
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
                        session.sendMessage(new TextMessage(data));
                        queue.poll();
                    } else {
                        logger.error("Session is closed. Unable to deliver message");
                        queue.clear();
                        return;
                    }
                    logger.debug("Session {}: {} messages left", session.getId(), queue.size());
                }
            }
        } catch (IOException e) {
            logger.error("Unexpected exception", e);
            throw new RuntimeException(e);
        } finally {
            if (acquired) {
                HiveWebsocketSessionState.get(session).getQueueLock().unlock();
            }
        }
    }

}



