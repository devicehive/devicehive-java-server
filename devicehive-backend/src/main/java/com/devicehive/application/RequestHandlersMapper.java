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

import com.devicehive.api.HandlersMapper;
import com.devicehive.messages.handler.DeviceDeleteHandler;
import com.devicehive.messages.handler.PluginSubscribeRequestHandler;
import com.devicehive.messages.handler.PluginUnsubscribeRequestHandler;
import com.devicehive.messages.handler.command.*;
import com.devicehive.messages.handler.dao.count.*;
import com.devicehive.messages.handler.dao.list.*;
import com.devicehive.messages.handler.notification.NotificationSubscribeRequestHandler;
import com.devicehive.messages.handler.command.CommandUnsubscribeRequestHandler;
import com.devicehive.messages.handler.notification.NotificationInsertHandler;
import com.devicehive.messages.handler.notification.NotificationSearchHandler;
import com.devicehive.messages.handler.notification.NotificationUnsubscribeRequestHandler;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class RequestHandlersMapper implements HandlersMapper {

    private final NotificationSearchHandler notificationSearchHandler;
    private final NotificationInsertHandler notificationInsertHandler;
    private final NotificationSubscribeRequestHandler notificationSubscribeRequestHandler;
    private final NotificationUnsubscribeRequestHandler notificationUnsubscribeRequestHandler;
    private final CommandInsertHandler commandInsertHandler;
    private final CommandSearchHandler commandSearchHandler;
    private final CommandUpdateHandler commandUpdateHandler;
    private final CommandsUpdateHandler commandsUpdateHandler;
    private final CommandSubscribeRequestHandler commandSubscribeRequestHandler;
    private final CommandUnsubscribeRequestHandler commandUnsubscribeRequestHandler;
    private final CommandUpdateSubscribeRequestHandler commandUpdateSubscribeRequestHandler;
    private final ListUserHandler listUserHandler;
    private final CountUserHandler countUserHandler;
    private final ListNetworkHandler listNetworkHandler;
    private final ListDeviceTypeHandler listDeviceTypeHandler;
    private final CountDeviceTypeHandler countDeviceTypeHandler;
    private final CountNetworkHandler countNetworkHandler;
    private final ListDeviceHandler listDeviceHandler;
    private final CountDeviceHandler countDeviceHandler;
    private final DeviceDeleteHandler deviceDeleteHandler;
    private final PluginSubscribeRequestHandler pluginSubscribeRequestHandler;
    private final PluginUnsubscribeRequestHandler pluginUnsubscribeRequestHandler;
    private final ListPluginHandler listPluginHandler;
    private final CountPluginHandler countPluginHandler;

    private Map<Action, RequestHandler> requestHandlerMap;

    //TODO - too many parameters, consider refactoring. Is it possible to find implementation without explicit map?
    @Autowired
    public RequestHandlersMapper(CommandUpdateHandler commandUpdateHandler,
                                 NotificationSearchHandler notificationSearchHandler,
                                 NotificationInsertHandler notificationInsertHandler,
                                 ListUserHandler listUserHandler,
                                 ListDeviceHandler listDeviceHandler,
                                 NotificationSubscribeRequestHandler notificationSubscribeRequestHandler,
                                 NotificationUnsubscribeRequestHandler notificationUnsubscribeRequestHandler,
                                 CommandInsertHandler commandInsertHandler,
                                 CommandSearchHandler commandSearchHandler,
                                 CommandsUpdateHandler commandsUpdateHandler,
                                 ListNetworkHandler listNetworkHandler,
                                 ListDeviceTypeHandler listDeviceTypeHandler,
                                 DeviceDeleteHandler deviceDeleteHandler,
                                 CommandSubscribeRequestHandler commandSubscribeRequestHandler,
                                 CommandUnsubscribeRequestHandler commandUnsubscribeRequestHandler,
                                 CommandUpdateSubscribeRequestHandler commandUpdateSubscribeRequestHandler,
                                 CountUserHandler countUserHandler,
                                 CountDeviceTypeHandler countDeviceTypeHandler,
                                 CountNetworkHandler countNetworkHandler,
                                 CountDeviceHandler countDeviceHandler,
                                 PluginSubscribeRequestHandler pluginSubscribeRequestHandler,
                                 PluginUnsubscribeRequestHandler pluginUnsubscribeRequestHandler,
                                 ListPluginHandler listPluginHandler,
                                 CountPluginHandler countPluginHandler) {
        this.commandUpdateHandler = commandUpdateHandler;
        this.notificationSearchHandler = notificationSearchHandler;
        this.notificationInsertHandler = notificationInsertHandler;
        this.listUserHandler = listUserHandler;
        this.listDeviceHandler = listDeviceHandler;
        this.notificationSubscribeRequestHandler = notificationSubscribeRequestHandler;
        this.notificationUnsubscribeRequestHandler = notificationUnsubscribeRequestHandler;
        this.commandInsertHandler = commandInsertHandler;
        this.commandSearchHandler = commandSearchHandler;
        this.commandsUpdateHandler = commandsUpdateHandler;
        this.listNetworkHandler = listNetworkHandler;
        this.listDeviceTypeHandler = listDeviceTypeHandler;
        this.deviceDeleteHandler = deviceDeleteHandler;
        this.commandSubscribeRequestHandler = commandSubscribeRequestHandler;
        this.commandUnsubscribeRequestHandler = commandUnsubscribeRequestHandler;
        this.commandUpdateSubscribeRequestHandler = commandUpdateSubscribeRequestHandler;
        this.countUserHandler = countUserHandler;
        this.countDeviceTypeHandler = countDeviceTypeHandler;
        this.countNetworkHandler = countNetworkHandler;
        this.countDeviceHandler = countDeviceHandler;
        this.pluginSubscribeRequestHandler = pluginSubscribeRequestHandler;
        this.pluginUnsubscribeRequestHandler = pluginUnsubscribeRequestHandler;
        this.listPluginHandler = listPluginHandler;
        this.countPluginHandler = countPluginHandler;
    }

    @PostConstruct
    public void init() {
        requestHandlerMap = ImmutableMap.<Action, RequestHandler>builder()
                .put(Action.NOTIFICATION_SEARCH_REQUEST, notificationSearchHandler)
                .put(Action.NOTIFICATION_INSERT_REQUEST, notificationInsertHandler)
                .put(Action.NOTIFICATION_SUBSCRIBE_REQUEST, notificationSubscribeRequestHandler)
                .put(Action.NOTIFICATION_UNSUBSCRIBE_REQUEST, notificationUnsubscribeRequestHandler)
                .put(Action.COMMAND_INSERT_REQUEST, commandInsertHandler)
                .put(Action.COMMAND_SEARCH_REQUEST, commandSearchHandler)
                .put(Action.COMMAND_UPDATE_REQUEST, commandUpdateHandler)
                .put(Action.COMMANDS_UPDATE_REQUEST, commandsUpdateHandler)
                .put(Action.COMMAND_SUBSCRIBE_REQUEST, commandSubscribeRequestHandler)
                .put(Action.COMMAND_UNSUBSCRIBE_REQUEST, commandUnsubscribeRequestHandler)
                .put(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST, commandUpdateSubscribeRequestHandler)
                .put(Action.PLUGIN_SUBSCRIBE_REQUEST, pluginSubscribeRequestHandler)
                .put(Action.PLUGIN_UNSUBSCRIBE_REQUEST, pluginUnsubscribeRequestHandler)
                .put(Action.LIST_USER_REQUEST, listUserHandler)
                .put(Action.COUNT_USER_REQUEST, countUserHandler)
                .put(Action.LIST_NETWORK_REQUEST, listNetworkHandler)
                .put(Action.LIST_DEVICE_TYPE_REQUEST, listDeviceTypeHandler)
                .put(Action.COUNT_DEVICE_TYPE_REQUEST, countDeviceTypeHandler)
                .put(Action.COUNT_NETWORK_REQUEST, countNetworkHandler)
                .put(Action.LIST_DEVICE_REQUEST, listDeviceHandler)
                .put(Action.COUNT_DEVICE_REQUEST, countDeviceHandler)
                .put(Action.DEVICE_DELETE_REQUEST, deviceDeleteHandler)
                .put(Action.LIST_PLUGIN_REQUEST, listPluginHandler)
                .put(Action.COUNT_PLUGIN_REQUEST, countPluginHandler)
                .build();
    }

    @Override
    public Map<Action, RequestHandler> requestHandlerMap() {
        return requestHandlerMap;
    }
}
