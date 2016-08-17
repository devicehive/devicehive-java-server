package com.devicehive.base;

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

import com.devicehive.application.websocket.WebSocketAuthenticationManager;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.json.GsonFactory;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.handlers.WebsocketExecutor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is not thread safe!
 */
public abstract class AbstractWebSocketMethodTest extends AbstractResourceTest {

    protected HiveWebsocketSessionState state;

    @Autowired
    private WebsocketExecutor executor;

    @Autowired
    private UserService userService;

    @Autowired
    private AccessKeyService accessKeyService;

    @Autowired
    private WebSocketAuthenticationManager webSocketAuthenticationManager;

    @Before
    public void createState() {
        state = new HiveWebsocketSessionState();
    }

    public HiveAuthentication auth(String login, String password) {
        HiveAuthentication.HiveAuthDetails details = new HiveAuthentication.HiveAuthDetails(null, null, null);
        HiveAuthentication authentication = webSocketAuthenticationManager.authenticateUser(login, password, details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        return authentication;
    }

    public HiveAuthentication auth(String key) {
        HiveAuthentication.HiveAuthDetails details = new HiveAuthentication.HiveAuthDetails(null, null, null);
        HiveAuthentication authentication = webSocketAuthenticationManager.authenticateKey(key, details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        return authentication;
    }

    public HiveAuthentication auth() {
        HiveAuthentication.HiveAuthDetails details = new HiveAuthentication.HiveAuthDetails(null, null, null);
        HiveAuthentication authentication = webSocketAuthenticationManager.authenticateAnonymous(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal((HivePrincipal) authentication.getPrincipal());
        return authentication;
    }

    public String runMethod(Object obj, Object auth) {
        String jsonObject = gson.toJson(obj);
        JsonObject jsonTree = new JsonParser().parse(jsonObject).getAsJsonObject();

        Map<String, Object> sessionDetails = new HashMap<>();
        sessionDetails.put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, auth);
        sessionDetails.put(HiveWebsocketSessionState.KEY, state);
        JsonObject result = executor.execute(jsonTree, new MyWebSocketSession(sessionDetails));
        return GsonFactory.createGson().toJson(result);
    }


    static class MyWebSocketSession implements WebSocketSession {

        private Map<String, Object> sessionAttributes;

        public MyWebSocketSession(Map<String, Object> sessionAttributes) {
            this.sessionAttributes = sessionAttributes;
        }

        @Override
        public String getId() {
            return "!";
        }

        @Override
        public URI getUri() {
            return null;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return new HttpHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return sessionAttributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 8080);
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {

        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return null;
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {

        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void close(CloseStatus status) throws IOException {
            close();
        }
    }

}
