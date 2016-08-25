package com.devicehive.websockets.handlers;

import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class CommandHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandlers.class);

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandSubscribe(JsonObject request,
                                                     WebSocketSession session) {
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUnsubscribe(JsonObject request,
                                                       WebSocketSession session) {
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'CREATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandInsert(JsonObject request,
                                                  WebSocketSession session) {
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'UPDATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUpdate(JsonObject request,
                                                  WebSocketSession session) {
        return new WebSocketResponse();
    }
}
