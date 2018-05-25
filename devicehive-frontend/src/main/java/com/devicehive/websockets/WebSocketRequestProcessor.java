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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.DEVICE_TYPE_ID;
import static com.devicehive.configuration.Constants.NETWORK_ID;

@Component
public class WebSocketRequestProcessor {

    private final CommonHandlers commonHandlers;
    private final ApiInfoHandlers apiInfoHandlers;
    private final ConfigurationHandlers configurationHandlers;
    private final NotificationHandlers notificationHandlers;
    private final CommandHandlers commandHandlers;
    private final SubscriptionHandlers subscriptionHandlers;
    private final DeviceHandlers deviceHandlers;
    private final NetworkHandlers networkHandlers;
    private final DeviceTypeHandlers deviceTypeHandlers;
    private final UserHandlers userHandlers;
    private final Gson gson;

    @Autowired
    public WebSocketRequestProcessor(CommonHandlers commonHandlers,
                                     ApiInfoHandlers apiInfoHandlers,
                                     ConfigurationHandlers configurationHandlers,
                                     NotificationHandlers notificationHandlers,
                                     CommandHandlers commandHandlers,
                                     SubscriptionHandlers subscriptionHandlers,
                                     DeviceHandlers deviceHandlers,
                                     NetworkHandlers networkHandlers,
                                     DeviceTypeHandlers deviceTypeHandlers,
                                     UserHandlers userHandlers,
                                     Gson gson) {
        this.commonHandlers = commonHandlers;
        this.apiInfoHandlers = apiInfoHandlers;
        this.configurationHandlers = configurationHandlers;
        this.notificationHandlers = notificationHandlers;
        this.commandHandlers = commandHandlers;
        this.subscriptionHandlers = subscriptionHandlers;
        this.deviceHandlers = deviceHandlers;
        this.networkHandlers = networkHandlers;
        this.deviceTypeHandlers = deviceTypeHandlers;
        this.userHandlers = userHandlers;
        this.gson = gson;
    }

    public void process(JsonObject request, WebSocketSession session) throws InterruptedException, IOException, HiveException {
        WebsocketAction action = getAction(request);
        final String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
        final Long networkId = gson.fromJson(request.get(NETWORK_ID), Long.class);
        final Long deviceTypeId = gson.fromJson(request.get(DEVICE_TYPE_ID), Long.class);
        
        switch (action) {
            case SERVER_INFO:
                apiInfoHandlers.processServerInfo(request, session);
                break;
            case SERVER_CACHE_INFO:
                apiInfoHandlers.processServerCacheInfo(request, session);
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
                notificationHandlers.processNotificationInsert(deviceId, request, session);
                break;
            case NOTIFICATION_SUBSCRIBE:
                notificationHandlers.processNotificationSubscribe(deviceId, request, session);
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
                commandHandlers.processCommandInsert(deviceId, request, session);
                break;
            case COMMAND_UPDATE:
                commandHandlers.processCommandUpdate(deviceId, request, session);
                break;
            case COMMAND_SUBSCRIBE:
                commandHandlers.processCommandSubscribe(deviceId, request, session);
                break;
            case COMMAND_UNSUBSCRIBE:
                commandHandlers.processCommandUnsubscribe(request, session);
                break;
            case COMMAND_GET:
                commandHandlers.processCommandGet(deviceId, request, session);
                break;
            case COMMAND_LIST:
                commandHandlers.processCommandList(deviceId, request, session);
                break;
            case SUBSCRIPTION_LIST:
                subscriptionHandlers.processSubscribeList(request, session);
                break;
            case DEVICE_GET:
                deviceHandlers.processDeviceGet(deviceId, request, session);
                break;
            case DEVICE_LIST:
                deviceHandlers.processDeviceList(request, session);
                break;
            case DEVICE_COUNT:
                deviceHandlers.processDeviceCount(request, session);
                break;
            case DEVICE_SAVE:
                deviceHandlers.processDeviceSave(deviceId, request, session);
                break;
            case DEVICE_DELETE:
                deviceHandlers.processDeviceDelete(deviceId, request, session);
                break;
            case NETWORK_LIST:
                networkHandlers.processNetworkList(request, session);
                break;
            case NETWORK_COUNT:
                networkHandlers.processNetworkCount(request, session);
                break;
            case NETWORK_GET:
                networkHandlers.processNetworkGet(networkId, request, session);
                break;
            case NETWORK_INSERT:
                networkHandlers.processNetworkInsert(request, session);
                break;
            case NETWORK_UPDATE:
                networkHandlers.processNetworkUpdate(networkId, request, session);
                break;
            case NETWORK_DELETE:
                networkHandlers.processNetworkDelete(networkId, request, session);
                break;
            case DEVICE_TYPE_LIST:
                deviceTypeHandlers.processDeviceTypeList(request, session);
                break;
            case DEVICE_TYPE_COUNT:
                deviceTypeHandlers.processDeviceTypeCount(request, session);
                break;
            case DEVICE_TYPE_GET:
                deviceTypeHandlers.processDeviceTypeGet(deviceTypeId, request, session);
                break;
            case DEVICE_TYPE_INSERT:
                deviceTypeHandlers.processDeviceTypeInsert(request, session);
                break;
            case DEVICE_TYPE_UPDATE:
                deviceTypeHandlers.processDeviceTypeUpdate(deviceTypeId, request, session);
                break;
            case DEVICE_TYPE_DELETE:
                deviceTypeHandlers.processDeviceTypeDelete(deviceTypeId, request, session);
                break;
            case USER_LIST:
                userHandlers.processUserList(request, session);
                break;
            case USER_COUNT:
                userHandlers.processUserCount(request, session);
                break;
            case USER_GET:
                userHandlers.processUserGet(request, session);
                break;
            case USER_INSERT:
                userHandlers.processUserInsert(request, session);
                break;
            case USER_UPDATE:
                userHandlers.processUserUpdate(request, session);
                break;
            case USER_GET_CURRENT:
                userHandlers.processUserGetCurrent(request, session);
                break;
            case USER_UPDATE_CURRENT:
                userHandlers.processUserUpdateCurrent(request, session);
                break;
            case USER_DELETE:
                userHandlers.processUserDelete(request, session);
                break;
            case USER_GET_NETWORK:
                userHandlers.processUserGetNetwork(request, session);
                break;
            case USER_ASSIGN_NETWORK:
                userHandlers.processUserAssignNetwork(request, session);
                break;
            case USER_UNASSIGN_NETWORK:
                userHandlers.processUserUnassignNetwork(request, session);
                break;
            case USER_GET_DEVICE_TYPE:
                userHandlers.processUserGetDeviceType(request, session);
                break;
            case USER_GET_DEVICE_TYPES:
                userHandlers.processUserGetDeviceTypes(request, session);
                break;
            case USER_ASSIGN_DEVICE_TYPE:
                userHandlers.processUserAssignDeviceType(request, session);
                break;
            case USER_UNASSIGN_DEVICE_TYPE:
                userHandlers.processUserUnassignDeviceType(request, session);
                break;
            case USER_ALLOW_ALL_DEVICE_TYPES:
                userHandlers.processUserAllowAllDeviceTypes(request, session);
                break;
            case USER_DISALLOW_ALL_DEVICE_TYPES:
                userHandlers.processUserDisallowAllDeviceTypes(request, session);
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
        SERVER_CACHE_INFO("server/cacheInfo"),
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
        SUBSCRIPTION_LIST("subscription/list"),
        DEVICE_GET("device/get"),
        DEVICE_LIST("device/list"),
        DEVICE_COUNT("device/count"),
        DEVICE_SAVE("device/save"),
        DEVICE_DELETE("device/delete"),
        NETWORK_LIST("network/list"),
        NETWORK_COUNT("network/count"),
        NETWORK_INSERT("network/insert"),
        NETWORK_GET("network/get"),
        NETWORK_DELETE("network/delete"),
        NETWORK_UPDATE("network/update"),
        DEVICE_TYPE_LIST("devicetype/list"),
        DEVICE_TYPE_COUNT("devicetype/count"),
        DEVICE_TYPE_INSERT("devicetype/insert"),
        DEVICE_TYPE_GET("devicetype/get"),
        DEVICE_TYPE_DELETE("devicetype/delete"),
        DEVICE_TYPE_UPDATE("devicetype/update"),
        USER_LIST("user/list"),
        USER_COUNT("user/count"),
        USER_GET("user/get"),
        USER_INSERT("user/insert"),
        USER_UPDATE("user/update"),
        USER_GET_CURRENT("user/getCurrent"),
        USER_UPDATE_CURRENT("user/updateCurrent"),
        USER_DELETE("user/delete"),
        USER_GET_NETWORK("user/getNetwork"),
        USER_ASSIGN_NETWORK("user/assignNetwork"),
        USER_UNASSIGN_NETWORK("user/unassignNetwork"),
        USER_GET_DEVICE_TYPE("user/getDeviceType"),
        USER_GET_DEVICE_TYPES("user/getDeviceTypes"),
        USER_ASSIGN_DEVICE_TYPE("user/assignDeviceType"),
        USER_UNASSIGN_DEVICE_TYPE("user/unassignDeviceType"),
        USER_ALLOW_ALL_DEVICE_TYPES("user/allowAllDeviceTypes"),
        USER_DISALLOW_ALL_DEVICE_TYPES("user/disallowAllDeviceTypes"),
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
