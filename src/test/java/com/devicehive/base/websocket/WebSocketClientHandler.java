package com.devicehive.base.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class WebSocketClientHandler extends TextWebSocketHandler {

    private BlockingQueue<TextMessage> messages;
    private WebSocketSession session;

    public WebSocketClientHandler(BlockingQueue<TextMessage> messages) {
        this.messages = messages;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        this.messages.put(new TextMessage("connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        this.messages.put(message);
    }

    public void sendMessage(TextMessage message) throws IOException {
        session.sendMessage(message);
    }

}

