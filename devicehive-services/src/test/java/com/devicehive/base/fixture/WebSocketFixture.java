package com.devicehive.base.fixture;

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

import com.devicehive.base.websocket.WebSocketSynchronousConnection;
import com.devicehive.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

import static com.devicehive.base.websocket.WebSocketSynchronousConnection.WAIT_TIMEOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WebSocketFixture {

    private static final Gson gson = GsonFactory.createGson();

    public static void authenticateUser(String login, String pass, WebSocketSynchronousConnection conn) throws IOException, InterruptedException {
        String request = RandomStringUtils.random(5);
        JsonObject authRequest = JsonFixture.userAuthCommand(request, login, pass);
        TextMessage response = conn.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is(request));
        assertThat(authResponse.get("status").getAsString(), is("success"));
    }

    public static void authenticateKey(String key, WebSocketSynchronousConnection conn) throws IOException, InterruptedException {
        String request = RandomStringUtils.random(5);
        JsonObject authRequest = JsonFixture.keyAuthCommand(request, key);
        TextMessage response = conn.sendMessage(new TextMessage(gson.toJson(authRequest)), WAIT_TIMEOUT);
        JsonObject authResponse = gson.fromJson(response.getPayload(), JsonObject.class);
        assertThat(authResponse.get("action").getAsString(), is("authenticate"));
        assertThat(authResponse.get("requestId").getAsString(), is(request));
        assertThat(authResponse.get("status").getAsString(), is("success"));
    }
}
