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

public class CountNetworkRequest extends Body {

    private String name;
    private String namePattern;
    private HivePrincipal principal;

    public CountNetworkRequest() {
        super(Action.COUNT_NETWORK_REQUEST);
    }

    public CountNetworkRequest(String name, String namePattern, HivePrincipal principal) {
        super(Action.COUNT_NETWORK_REQUEST);
        this.name = name;
        this.namePattern = namePattern;
        this.principal = principal;
    }

    public static CountNetworkRequest createCountNetworkRequest(JsonObject request, HivePrincipal principal) {
        final CountNetworkRequest countNetworkRequest = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, CountNetworkRequest.class);

        countNetworkRequest.setPrincipal(principal);

        return countNetworkRequest;
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

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(HivePrincipal principal) {
        this.principal = principal;
    }
}
