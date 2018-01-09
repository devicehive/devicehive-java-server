package com.devicehive.model.rpc;

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

import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.lang.reflect.Modifier;

public class CountUserRequest extends Body {

    private String login;
    private String loginPattern;
    private Integer role;
    private Integer status;

    public CountUserRequest() {
        super(Action.COUNT_USER_REQUEST);
    }

    public CountUserRequest(String login, String loginPattern, Integer role, Integer status) {
        super(Action.COUNT_USER_REQUEST);
        this.login = login;
        this.loginPattern = loginPattern;
        this.role = role;
        this.status = status;
    }

    public static CountUserRequest createCountUserRequest(JsonObject request) {
        final CountUserRequest countUserRequest = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, CountUserRequest.class);

        return countUserRequest;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getLoginPattern() {
        return loginPattern;
    }

    public void setLoginPattern(String loginPattern) {
        this.loginPattern = loginPattern;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
