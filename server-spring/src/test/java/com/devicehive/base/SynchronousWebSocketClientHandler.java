package com.devicehive.base;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SynchronousWebSocketClientHandler extends TextWebSocketHandler {

    private BlockingQueue<TextMessage> messages = new SynchronousQueue<>();

    public WebSocketSession session;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        this.messages.put(new TextMessage("connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        this.messages.put(message);
    }

    public void awaitMessage(long seconds, TextMessage expectedMessage) throws InterruptedException {
        Object message = this.messages.poll(seconds, TimeUnit.SECONDS);
        assertEquals(expectedMessage, message);
    }

    public TextMessage awaitMessage(long seconds) throws InterruptedException {
        return this.messages.poll(seconds, TimeUnit.SECONDS);
    }

    public void sendMessage(TextMessage message) throws IOException {
        session.sendMessage(message);
    }

}

