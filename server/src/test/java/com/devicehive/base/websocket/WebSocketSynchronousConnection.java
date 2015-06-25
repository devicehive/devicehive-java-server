package com.devicehive.base.websocket;

import org.junit.Assert;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class WebSocketSynchronousConnection {
    public static final int WAIT_TIMEOUT = 3;

    private BlockingQueue<TextMessage> messages;
    private WebSocketClientHandler handler;
    private WebSocketConnectionManager connectionManager;

    public WebSocketSynchronousConnection() {
        messages = new SynchronousQueue<>();
    }

    public void start(String wsUri) throws InterruptedException {
        StandardWebSocketClient client = new StandardWebSocketClient();
        handler = new WebSocketClientHandler(messages);
        connectionManager = new WebSocketConnectionManager(client, handler, wsUri);
        connectionManager.start();

        TextMessage message = messages.poll(WAIT_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(new TextMessage("connected"), message);
    }

    public void stop() {
        connectionManager.stop();
    }

    public TextMessage sendMessage(TextMessage message, long waitSec) throws IOException, InterruptedException {
        handler.sendMessage(message);
        return messages.poll(waitSec, TimeUnit.SECONDS);
    }

}
