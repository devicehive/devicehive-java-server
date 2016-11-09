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

import com.devicehive.handler.command.*;
import com.devicehive.handler.dao.list.*;
import com.devicehive.handler.notification.NotificationSubscribeRequestHandler;
import com.devicehive.handler.command.CommandUnsubscribeRequestHandler;
import com.devicehive.handler.notification.NotificationInsertHandler;
import com.devicehive.handler.notification.NotificationSearchHandler;
import com.devicehive.handler.notification.NotificationUnsubscribeRequestHandler;
import com.devicehive.model.rpc.Action;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ComponentScan("com.devicehive.handler")
public class RequestHandlersConfig {

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void init() {
        requestHandlerMap().values().forEach(x -> context.getAutowireCapableBeanFactory().autowireBean(x));
    }

    @Bean
    public Map<Action, RequestHandler> requestHandlerMap() {
        return new HashMap<Action, RequestHandler>() {{
            put(Action.NOTIFICATION_SEARCH_REQUEST, new NotificationSearchHandler());
            put(Action.NOTIFICATION_INSERT_REQUEST, new NotificationInsertHandler());
            put(Action.NOTIFICATION_SUBSCRIBE_REQUEST, new NotificationSubscribeRequestHandler());
            put(Action.NOTIFICATION_UNSUBSCRIBE_REQUEST, new NotificationUnsubscribeRequestHandler());
            put(Action.COMMAND_INSERT_REQUEST, new CommandInsertHandler());
            put(Action.COMMAND_SEARCH_REQUEST, new CommandSearchHandler());
            put(Action.COMMAND_UPDATE_REQUEST, new CommandUpdateRequestHandler());
            put(Action.COMMAND_SUBSCRIBE_REQUEST, new CommandSubscribeRequestHandler());
            put(Action.COMMAND_UNSUBSCRIBE_REQUEST, new CommandUnsubscribeRequestHandler());
            put(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST, new CommandUpdateSubscribeRequestHandler());
            put(Action.COMMAND_GET_SUBSCRIPTION_REQUEST, new CommandGetSubscriptionRequestHandler());

            put(Action.LIST_ACCESS_KEY_REQUEST, new ListAccessKeyHandler());

            put(Action.LIST_USER_REQUEST, new ListUserHandler());

            put(Action.LIST_NETWORK_REQUEST, new ListNetworkHandler());

            put(Action.LIST_DEVICE_REQUEST, new ListDeviceHandler());

            put(Action.LIST_DEVICE_CLASS_REQUEST, new ListDeviceClassHandler());
        }};
    }

}
