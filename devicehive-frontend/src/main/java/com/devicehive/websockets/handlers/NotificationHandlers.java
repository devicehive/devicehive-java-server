package com.devicehive.websockets.handlers;

import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class NotificationHandlers {

    private static final Logger logger = LoggerFactory.getLogger(NotificationHandlers.class);

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationSubscribe(JsonObject request,
                                                          WebSocketSession session) {
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationUnsubscribe(JsonObject request,
                                                            WebSocketSession session) {
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'CREATE_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationInsert(JsonObject request,
                                                       WebSocketSession session) {
        return new WebSocketResponse();
    }
}
