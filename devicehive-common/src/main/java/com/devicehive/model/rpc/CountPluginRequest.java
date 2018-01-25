package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.lang.reflect.Modifier;

public class CountPluginRequest extends Body {

    private String name;
    private String namePattern;
    private String topicName;
    private Integer status;
    private Long userId;
    private HivePrincipal principal;

    public CountPluginRequest() {
        super(Action.COUNT_PLUGIN_REQUEST);
    }

    public CountPluginRequest(String name, String namePattern, String topicName, Integer status, Long userId,
                              HivePrincipal principal) {
        super(Action.COUNT_PLUGIN_REQUEST);

        this.name = name;
        this.namePattern = namePattern;
        this.topicName = topicName;
        this.status = status;
        this.userId = userId;
        this.principal = principal;
    }

    public static CountPluginRequest createCountPluginRequest(JsonObject request, HivePrincipal principal) {
        CountPluginRequest countPluginRequest = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, CountPluginRequest.class);

        countPluginRequest.setPrincipal(principal);

        return countPluginRequest;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(HivePrincipal principal) {
        this.principal = principal;
    }

}