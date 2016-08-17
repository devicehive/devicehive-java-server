package com.devicehive.websockets.util;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stas on 06.05.14.
 */
@Component
public class SubscriptionSessionMap {

    @Autowired
    private SessionMonitor sessionMonitor;

    private ConcurrentMap<UUID, String> map = new ConcurrentHashMap<>();


    public void put(UUID subId, WebSocketSession session) {
        map.put(subId, session.getId());
    }

    public WebSocketSession get(UUID subId) {
        String sessionId = map.get(subId);
        if (sessionId != null) {
            return sessionMonitor.getSession(sessionId);
        }
        return null;
    }

    public void removeAll(Collection<UUID> uuids) {
        if (uuids != null) {
            for (UUID uuid : uuids) {
                map.remove(uuid);
            }
        }
    }

    public void remove(UUID uuid) {
        map.remove(uuid);
    }
}
