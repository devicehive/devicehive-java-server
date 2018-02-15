package com.devicehive.service;

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

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.SubscriptionInfo;
import com.devicehive.websockets.handlers.CommandHandlers;
import com.devicehive.websockets.handlers.NotificationHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.devicehive.configuration.Constants.COMMAND;
import static com.devicehive.configuration.Constants.NOTIFICATION;

@Component
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @SuppressWarnings("unchecked")
    public Set<SubscriptionInfo> list(String type, HivePrincipal principal, WebSocketSession session) {
        logger.debug("subscribe/list action. Session {} ", session.getId());

        Set<SubscriptionInfo> subscriptions = new HashSet<>();
        if (principal.getActions().contains(HiveAction.GET_DEVICE_COMMAND)) {
            if (type == null || type.equals(COMMAND)) {
                subscriptions.addAll(((CopyOnWriteArraySet<SubscriptionInfo>) session
                        .getAttributes()
                        .get(CommandHandlers.SUBSCRIPTION_SET_NAME)));
            }
        }
        if (principal.getActions().contains(HiveAction.GET_DEVICE_NOTIFICATION)) {
            if (type == null || type.equals(NOTIFICATION)) {
                subscriptions.addAll(((CopyOnWriteArraySet<SubscriptionInfo>) session
                        .getAttributes()
                        .get(NotificationHandlers.SUBSCRIPTION_SET_NAME)));
            }
        }

        return subscriptions;
    }
}
