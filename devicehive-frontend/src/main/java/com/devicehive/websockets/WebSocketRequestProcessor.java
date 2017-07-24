package com.devicehive.websockets;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.exceptions.HiveException;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.handlers.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class WebSocketRequestProcessor {

    @Autowired
    private CommonHandlers commonHandlers;
    @Autowired
    private ApiInfoHandlers apiInfoHandlers;
    @Autowired
    private ConfigurationHandlers configurationHandlers;
    @Autowired
    private NotificationHandlers notificationHandlers;
    @Autowired
    private CommandHandlers commandHandlers;
    @Autowired
    private DeviceHandlers deviceHandlers;

    public void process(JsonObject request, WebSocketSession session) throws InterruptedException, IOException {
        WebsocketAction action = getAction(request);
        switch (action) {
            case SERVER_INFO:
                apiInfoHandlers.processServerInfo(request, session);
                break;
            case CLUSTER_CONFIG_INFO:
                apiInfoHandlers.processClusterConfigInfo(request, session);
                break;    
            case AUTHENTICATE:
                commonHandlers.processAuthenticate(request, session);
                break;
            case TOKEN:
                commonHandlers.processLogin(request, session);
                break;
            case TOKEN_CREATE:
                commonHandlers.processTokenCreate(request, session);
                break;
            case TOKEN_REFRESH:
                commonHandlers.processRefresh(request, session);
                break;
            case CONFIGURATION_GET:
                configurationHandlers.processConfigurationGet(request, session);
                break;
            case CONFIGURATION_PUT:
                configurationHandlers.processConfigurationPut(request, session);
                break;
            case CONFIGURATION_DELETE:
                configurationHandlers.processConfigurationDelete(request, session);
                break;    
            case NOTIFICATION_INSERT:
                notificationHandlers.processNotificationInsert(request, session);
                break;
            case NOTIFICATION_SUBSCRIBE:
                notificationHandlers.processNotificationSubscribe(request, session);
                break;
            case NOTIFICATION_UNSUBSCRIBE:
                notificationHandlers.processNotificationUnsubscribe(request, session);
                break;
            case NOTIFICATION_GET:
                notificationHandlers.processNotificationGet(request, session);
                break;
            case NOTIFICATION_LIST:
                notificationHandlers.processNotificationList(request, session);
                break;
            case COMMAND_INSERT:
                commandHandlers.processCommandInsert(request, session);
                break;
            case COMMAND_UPDATE:
                commandHandlers.processCommandUpdate(request, session);
                break;
            case COMMAND_SUBSCRIBE:
                commandHandlers.processCommandSubscribe(request, session);
                break;
            case COMMAND_UNSUBSCRIBE:
                commandHandlers.processCommandUnsubscribe(request, session);
                break;
            case COMMAND_GET:
                commandHandlers.processCommandGet(request, session);
                break;
            case COMMAND_LIST:
                commandHandlers.processCommandList(request, session);
                break;
            case DEVICE_GET:
                deviceHandlers.processDeviceGet(request, session);
                break;
            case DEVICE_LIST:
                deviceHandlers.processDeviceList(request, session);
                break;
            case DEVICE_SAVE:
                deviceHandlers.processDeviceSave(request, session);
                break;
            case DEVICE_DELETE:
                deviceHandlers.processDeviceDelete(request, session);
                break;
            case EMPTY: default:
                throw new JsonParseException("'action' field could not be parsed to known endpoint");
        }
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
        CLUSTER_CONFIG_INFO("cluster/info"),
        AUTHENTICATE("authenticate"),
        TOKEN("token"),
        TOKEN_CREATE("token/create"),
        TOKEN_REFRESH("token/refresh"),
        CONFIGURATION_GET("configuration/get"),
        CONFIGURATION_PUT("configuration/put"),
        CONFIGURATION_DELETE("configuration/delete"),
        NOTIFICATION_INSERT("notification/insert"),
        NOTIFICATION_SUBSCRIBE("notification/subscribe"),
        NOTIFICATION_UNSUBSCRIBE("notification/unsubscribe"),
        NOTIFICATION_GET("notification/get"),
        NOTIFICATION_LIST("notification/list"),
        COMMAND_INSERT("command/insert"),
        COMMAND_SUBSCRIBE("command/subscribe"),
        COMMAND_UNSUBSCRIBE("command/unsubscribe"),
        COMMAND_UPDATE("command/update"),
        COMMAND_GET("command/get"),
        COMMAND_LIST("command/list"),
        DEVICE_GET("device/get"),
        DEVICE_LIST("device/list"),
        DEVICE_SAVE("device/save"),
        DEVICE_DELETE("device/delete"),
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
