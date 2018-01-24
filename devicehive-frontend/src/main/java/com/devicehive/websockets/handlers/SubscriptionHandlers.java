package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.SubscriptionInfo;
import com.devicehive.service.SubscriptionService;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

import static com.devicehive.configuration.Constants.*;

@Component
public class SubscriptionHandlers {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionHandlers.class);

    private final Gson gson;
    private final SubscriptionService subscriptionService;
    private final WebSocketClientHandler clientHandler;

    @Autowired
    public SubscriptionHandlers(Gson gson,
                                SubscriptionService subscriptionService,
                                WebSocketClientHandler clientHandler) {
        this.gson = gson;
        this.subscriptionService = subscriptionService;
        this.clientHandler = clientHandler;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and (hasPermission(null, 'GET_DEVICE_COMMAND')" +
            " or hasPermission(null, 'GET_DEVICE_NOTIFICATION'))")
    @SuppressWarnings("unchecked")
    public void processSubscribeList(JsonObject request, WebSocketSession session) {
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String type = gson.fromJson(request.get(TYPE), String.class);

        Set<SubscriptionInfo> subscriptions = subscriptionService.list(type, principal, session);

        logger.debug("subscribe/list completed for session {}", session.getId());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTIONS, subscriptions, JsonPolicyDef.Policy.SUBSCRIPTIONS_LISTED);
        clientHandler.sendMessage(request, response, session);
    }
}
