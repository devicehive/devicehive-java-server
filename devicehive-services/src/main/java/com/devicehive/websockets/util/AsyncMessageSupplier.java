package com.devicehive.websockets.util;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.configuration.Constants;
import com.devicehive.json.GsonFactory;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class AsyncMessageSupplier {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageSupplier.class);

    public static final JsonElement PING_JSON_MSG = new JsonArray();

    @Async(DeviceHiveApplication.MESSAGE_EXECUTOR)
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
                        WebSocketMessage<?> webSocketMessage = null;
                        if (jsonElement == PING_JSON_MSG) {
                            webSocketMessage = new PingMessage(Constants.PING);
                        } else {
                            String data = GsonFactory.createGson().toJson(jsonElement);
                            webSocketMessage = new TextMessage(data);
                        }
                        session.sendMessage(webSocketMessage);
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



