package com.devicehive.websockets.util;

import com.devicehive.configuration.Constants;
import com.devicehive.websockets.events.WSMessageEvent;
import com.google.gson.JsonElement;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import static com.devicehive.websockets.util.SessionMonitor.PING_JSON_MSG;

@Component
public class WSMessageEventHandler implements EventHandler<WSMessageEvent> {

    private static final Logger logger = LoggerFactory.getLogger(WSMessageEventHandler.class);

    @Override
    public void onEvent(WSMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        JsonElement message = event.getMessage();
        WebSocketSession session = event.getSession();
        if (session.isOpen()) {
            WebSocketMessage<?> webSocketMessage;
            if (PING_JSON_MSG.equals(message)) {
                webSocketMessage = new PingMessage(Constants.PING);
            } else {
                webSocketMessage = new TextMessage(message.toString());
            }
            session.sendMessage(webSocketMessage);
        } else {
            logger.error("Session is closed. Unable to deliver message");
        }
    }
}
