package com.devicehive.application;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.handler.DeviceCreateHandler;
import com.devicehive.handler.command.*;
import com.devicehive.handler.dao.list.*;
import com.devicehive.handler.notification.NotificationSubscribeRequestHandler;
import com.devicehive.handler.command.CommandUnsubscribeRequestHandler;
import com.devicehive.handler.notification.NotificationInsertHandler;
import com.devicehive.handler.notification.NotificationSearchHandler;
import com.devicehive.handler.notification.NotificationUnsubscribeRequestHandler;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class RequestHandlersConfig {

    @Autowired
    private NotificationSearchHandler notificationSearchHandler;
    @Autowired
    private NotificationInsertHandler notificationInsertHandler;
    @Autowired
    private NotificationSubscribeRequestHandler notificationSubscribeRequestHandler;
    @Autowired
    private NotificationUnsubscribeRequestHandler notificationUnsubscribeRequestHandler;
    @Autowired
    private CommandInsertHandler commandInsertHandler;
    @Autowired
    private CommandSearchHandler commandSearchHandler;
    @Autowired
    private CommandUpdateHandler commandUpdateHandler;
    @Autowired
    private CommandsUpdateHandler commandsUpdateHandler;
    @Autowired
    private CommandSubscribeRequestHandler commandSubscribeRequestHandler;
    @Autowired
    private CommandUnsubscribeRequestHandler commandUnsubscribeRequestHandler;
    @Autowired
    private CommandUpdateSubscribeRequestHandler commandUpdateSubscribeRequestHandler;
    @Autowired
    private CommandGetSubscriptionRequestHandler commandGetSubscriptionRequestHandler;
    @Autowired
    private ListUserHandler listUserHandler;
    @Autowired
    private ListNetworkHandler listNetworkHandler;
    @Autowired
    private ListDeviceHandler listDeviceHandler;
    @Autowired
    private DeviceCreateHandler deviceCreateHandler;

    private Map<Action, RequestHandler> requestHandlerMap;

    @PostConstruct
    public void init() {
        requestHandlerMap = new HashMap<Action, RequestHandler>() {{
            put(Action.NOTIFICATION_SEARCH_REQUEST, notificationSearchHandler);
            put(Action.NOTIFICATION_INSERT_REQUEST, notificationInsertHandler);
            put(Action.NOTIFICATION_SUBSCRIBE_REQUEST, notificationSubscribeRequestHandler);
            put(Action.NOTIFICATION_UNSUBSCRIBE_REQUEST, notificationUnsubscribeRequestHandler);
            put(Action.COMMAND_INSERT_REQUEST, commandInsertHandler);
            put(Action.COMMAND_SEARCH_REQUEST, commandSearchHandler);
            put(Action.COMMAND_UPDATE_REQUEST, commandUpdateHandler);
            put(Action.COMMANDS_UPDATE_REQUEST, commandsUpdateHandler);
            put(Action.COMMAND_SUBSCRIBE_REQUEST, commandSubscribeRequestHandler);
            put(Action.COMMAND_UNSUBSCRIBE_REQUEST, commandUnsubscribeRequestHandler);
            put(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST, commandUpdateSubscribeRequestHandler);
            put(Action.COMMAND_GET_SUBSCRIPTION_REQUEST, commandGetSubscriptionRequestHandler);

            put(Action.LIST_USER_REQUEST, listUserHandler);

            put(Action.LIST_NETWORK_REQUEST, listNetworkHandler);

            put(Action.LIST_DEVICE_REQUEST, listDeviceHandler);

            put(Action.DEVICE_CREATE_REQUEST, deviceCreateHandler);
        }};
    }

    public Map<Action, RequestHandler> requestHandlerMap() {
        return requestHandlerMap;
    }

}
