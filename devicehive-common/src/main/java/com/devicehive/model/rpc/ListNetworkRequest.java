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
import com.devicehive.model.enums.SortOrder;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.lang.reflect.Modifier;
import java.util.Optional;

import static com.devicehive.configuration.Constants.DEFAULT_SKIP;
import static com.devicehive.configuration.Constants.DEFAULT_TAKE;

public class ListNetworkRequest extends Body {

    private String name;
    private String namePattern;
    private String sortField;
    private String sortOrder;
    private Integer take;
    private Integer skip;
    private Optional<HivePrincipal> principal;

    public ListNetworkRequest() {
        super(Action.LIST_NETWORK_REQUEST);
    }

    public static ListNetworkRequest createListNetworkRequest(JsonObject request) {
        ListNetworkRequest listNetworkRequest = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, ListNetworkRequest.class);
        listNetworkRequest.setTake(Optional.ofNullable(listNetworkRequest.getTake()).orElse(DEFAULT_TAKE));
        listNetworkRequest.setSkip(Optional.ofNullable(listNetworkRequest.getSkip()).orElse(DEFAULT_SKIP));

        return listNetworkRequest;
    }

    public boolean isSortOrderAsc() {
        return SortOrder.parse(sortOrder);
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

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }

    public Optional<HivePrincipal> getPrincipal() {
        return principal;
    }

    public void setPrincipal(Optional<HivePrincipal> principal) {
        this.principal = principal;
    }
}
