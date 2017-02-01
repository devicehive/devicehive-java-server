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

import com.devicehive.websockets.util.HiveEndpoint;
import org.springframework.web.socket.WebSocketSession;

public class ClientWebSocketHandler extends AbstractWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        HiveWebsocketSessionState state =
                (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, session.getPrincipal());
        state.setEndpoint(HiveEndpoint.CLIENT);
    }
}
