package com.devicehive.proxy.client;

/*
 * #%L
 * DeviceHive Proxy WebSocket Kafka Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.exceptions.HiveException;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessage;
import com.devicehive.proxy.api.payload.MessagePayload;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ClientEndpoint(
        decoders = GsonProxyMessageDecoder.class,
        encoders = GsonProxyMessageEncoder.class
)
public class WebSocketKafkaProxyClient extends ProxyClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketKafkaProxyClient.class);

    private WebSocketKafkaProxyConfig webSocketKafkaProxyConfig;
    private Map<String, CompletableFuture<ProxyMessage>> futureMap;
    private Map<String, Boolean> ackReceived;
    private Session session;

    public WebSocketKafkaProxyClient(NotificationHandler notificationHandler) {
        super(notificationHandler);
    }

    @Override
    public void start() {
        try {
            this.futureMap = new ConcurrentHashMap<>();
            if (webSocketKafkaProxyConfig.getAckEnable()) {
                this.ackReceived = new ConcurrentHashMap<>();
            }
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, new URI("ws://" + webSocketKafkaProxyConfig.getProxyConnect()));
        } catch (Exception e) {
            logger.error("Error during establishing connection: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            session.close();
        } catch (IOException e) {
            logger.error("Error during closing connection: ", e);
        }
    }

    @Override
    public CompletableFuture<ProxyMessage> push(ProxyMessage message) {
        this.session.getAsyncRemote().sendObject(message);

        CompletableFuture<ProxyMessage> future = new CompletableFuture<>();
        futureMap.put(message.getId(), future);
        logger.debug("Message {} was sent", message);
        return future;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("New WebSocket session established: {}", session.getId());
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        logger.info("WebSocket session {} closed, close code {}", session.getId(), reason.getCloseCode());
        this.session = null;
        futureMap.clear();
        if (webSocketKafkaProxyConfig.getAckEnable()) {
            ackReceived.clear();
        }
    }

    @OnMessage
    public void onMessage(List<ProxyMessage> messages) {
        messages.forEach(message -> {
            if (message.getStatus() == null || message.getStatus() != 0) {
                MessagePayload payload = (MessagePayload) message.getPayload();
                String msg = "Response message is failed: " + payload.getMessage();
                logger.warn(msg);
                throw new HiveException(msg);
            }

            String id = message.getId();
            if (id != null) {
                CompletableFuture<ProxyMessage> future = futureMap.get(id);
                if (future != null) {
                    if (webSocketKafkaProxyConfig.getAckEnable() && "ack".equals(message.getType())) {
                        if (message.getStatus() != 0) {
                            throw new HiveException("Acknowledgement failed for request id " + id);
                        }
                        ackReceived.put(id, true);
                        logger.debug("Acknowledgement message {} received for request id {}", message, id);
                    } else {
                        if (webSocketKafkaProxyConfig.getAckEnable() && !ackReceived.getOrDefault(id, false)) {
                            throw new HiveException("No acknowledgement received for request id " + id);
                        }
                        future.complete(message);
                        futureMap.remove(id);
                        if (webSocketKafkaProxyConfig.getAckEnable()) {
                            ackReceived.remove(id);
                        }
                    }
                }
            }

            if ("notif".equals(message.getType()) && message.getAction() == null) {
                MessagePayload payload = (MessagePayload) message.getPayload();
                notificationHandler.handle(payload.getMessage(), this);
            }
            logger.debug("Message {} was received", message);
        });
    }

    public void setWebSocketKafkaProxyConfig(WebSocketKafkaProxyConfig webSocketKafkaProxyConfig) {
        this.webSocketKafkaProxyConfig = webSocketKafkaProxyConfig;
    }
}
