package com.devicehive.base.websocket;

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

import org.junit.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class WebSocketSynchronousConnection {
    public static final int WAIT_TIMEOUT = 5;

    private BlockingQueue<TextMessage> messages;
    private WebSocketClientHandler handler;
    private WebSocketConnectionManager connectionManager;

    private volatile boolean started = false;
    private volatile boolean finished = false;
    private final Object connectSyncLock = new Object();
    private final Object closeSyncLock = new Object();

    public WebSocketSynchronousConnection() {
        messages = new SynchronousQueue<>();
    }

    public void start(String wsUri) throws InterruptedException {
        startInternal(wsUri);

        TextMessage message = messages.poll(WAIT_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(new TextMessage("connected"), message);
    }

    private void startInternal(String wsUri) throws InterruptedException {
        StandardWebSocketClient client = new StandardWebSocketClient();

        synchronized (connectSyncLock) {

            handler = new InternalListenable(messages);

            connectionManager = new WebSocketConnectionManager(client, handler, wsUri);
            connectionManager.start();

            int count = 100;
            while (!started && count > 0) {
                connectSyncLock.wait(50);
                count--;
            }
        }
    }

    public void stop() {
        try {
            synchronized (closeSyncLock) {
                connectionManager.stop();
                int count = 100;
                while (!finished && count > 0) {
                    closeSyncLock.wait(50);
                    count--;
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public TextMessage sendMessage(TextMessage message, long waitSec) throws IOException, InterruptedException {
        handler.sendMessage(message);
        return messages.poll(waitSec, TimeUnit.SECONDS);
    }

    public TextMessage pollMessage(long waitSec) throws InterruptedException {
        return messages.poll(waitSec, TimeUnit.SECONDS);
    }

    class InternalListenable extends WebSocketClientHandler {

        public InternalListenable(BlockingQueue<TextMessage> messages) {
            super(messages);
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            super.afterConnectionEstablished(session);
            synchronized (connectSyncLock) {
                started = true;
                connectSyncLock.notifyAll();
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            super.afterConnectionClosed(session, status);
            synchronized (closeSyncLock) {
                finished = true;
                closeSyncLock.notifyAll();
            }
        }
    }

}
