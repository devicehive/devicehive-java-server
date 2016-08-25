package com.devicehive.websockets;

import com.devicehive.exceptions.HiveException;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.CommandHandlers;
import com.devicehive.websockets.handlers.CommonHandlers;
import com.devicehive.websockets.handlers.DeviceHandlers;
import com.devicehive.websockets.handlers.NotificationHandlers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletResponse;

@Component
public class WebSocketRequestProcessor {

    @Autowired
    private CommonHandlers commonHandlers;
    @Autowired
    private NotificationHandlers notificationHandlers;
    @Autowired
    private CommandHandlers commandHandlers;
    @Autowired
    private DeviceHandlers deviceHandlers;

    public WebSocketResponse process(JsonObject request, WebSocketSession session) {
        WebSocketResponse response;
        WebsocketAction action = getAction(request);
        switch (action) {
            case SERVER_INFO:
                response = commonHandlers.processServerInfo(session);
                break;
            case AUTHENTICATE:
                response = commonHandlers.processAuthenticate(request, session);
                break;
            case NOTIFICATION_INSERT:
                response = notificationHandlers.processNotificationInsert(request, session);
                break;
            case NOTIFICATION_SUBSCRIBE:
                response = notificationHandlers.processNotificationSubscribe(request, session);
                break;
            case NOTIFICATION_UNSUBSCRIBE:
                response = notificationHandlers.processNotificationUnsubscribe(request, session);
                break;
            case COMMAND_INSERT:
                response = commandHandlers.processCommandInsert(request, session);
                break;
            case COMMAND_UPDATE:
                response = commandHandlers.processCommandUpdate(request, session);
                break;
            case COMMAND_SUBSCRIBE:
                response = commandHandlers.processCommandSubscribe(request, session);
                break;
            case COMMAND_UNSUBSCRIBE:
                response = commandHandlers.processCommandUnsubscribe(request, session);
                break;
            case DEVICE_GET:
                response = deviceHandlers.processDeviceGet(request);
                break;
            case DEVICE_SAVE:
                response = deviceHandlers.processDeviceSave(request, session);
                break;
            case EMPTY: default:
                throw new JsonParseException("'action' field could not be parsed to known endpoint");
        }
        return response;
    }

    private WebsocketAction getAction(JsonObject request) {
        JsonElement action = request.get(JsonMessageBuilder.ACTION);
        if (action == null || !action.isJsonPrimitive()) {
            return WebsocketAction.EMPTY;
        }
        return WebsocketAction.forName(action.getAsString());
    }

    public enum WebsocketAction {
        SERVER_INFO("server/info"),
        AUTHENTICATE("authenticate"),
        NOTIFICATION_INSERT("notification/insert"),
        NOTIFICATION_SUBSCRIBE("notification/subscribe"),
        NOTIFICATION_UNSUBSCRIBE("notification/unsubscribe"),
        COMMAND_INSERT("command/insert"),
        COMMAND_SUBSCRIBE("command/subscribe"),
        COMMAND_UNSUBSCRIBE("command/unsubscribe"),
        COMMAND_UPDATE("command/update"),
        DEVICE_GET("device/get"),
        DEVICE_SAVE("device/save"),
        EMPTY("");

        private String value;

        WebsocketAction(String method) {
            this.value = method;
        }

        public static WebsocketAction forName(String value) {
            for (WebsocketAction type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new HiveException("Illegal argument: " + value, HttpServletResponse.SC_BAD_REQUEST);

        }
    }
}
