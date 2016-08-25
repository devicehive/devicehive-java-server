package com.devicehive.websockets.handlers;

import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class DeviceHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHandlers.class);

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY')")
    public WebSocketResponse processDeviceGet(JsonObject request) {
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'REGISTER_DEVICE')")
    public WebSocketResponse processDeviceSave(JsonObject request,
                                               WebSocketSession session) {
        return new WebSocketResponse();
    }
}
