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
import com.devicehive.shim.api.Body;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.Optional;

import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.configuration.Constants.NAME_PATTERN;
import static com.devicehive.configuration.Constants.NETWORK_ID;
import static com.devicehive.configuration.Constants.NETWORK_NAME;
import static com.devicehive.configuration.Constants.SKIP;
import static com.devicehive.configuration.Constants.SORT_FIELD;
import static com.devicehive.configuration.Constants.SORT_ORDER;
import static com.devicehive.configuration.Constants.TAKE;

public class ListDeviceRequest extends Body {

    private String name;
    private String namePattern;
    private Long networkId;
    private String networkName;
    private String sortField;
    private boolean sortOrderAsc;
    private Integer take;
    private Integer skip;
    private HivePrincipal principal;

    public ListDeviceRequest() {
        super(Action.LIST_DEVICE_REQUEST.name());
    }

    public ListDeviceRequest(Long networkId) {
        super(Action.LIST_DEVICE_REQUEST.name());
        this.networkId = networkId;
    }

    public ListDeviceRequest(boolean sortOrderAsc, HivePrincipal principal) {
        super(Action.LIST_DEVICE_REQUEST.name());
        this.sortOrderAsc = sortOrderAsc;
        this.principal = principal;
    }

    public ListDeviceRequest(JsonObject request, HivePrincipal principal) {
        super(Action.LIST_DEVICE_REQUEST.name());
        name = Optional.ofNullable(request.get(NAME)).map(JsonElement::getAsString).orElse(null);
        namePattern = Optional.ofNullable(request.get(NAME_PATTERN)).map(JsonElement::getAsString).orElse(null);
        networkId = Optional.ofNullable(request.get(NETWORK_ID)).map(JsonElement::getAsLong).orElse(null);
        networkName = Optional.ofNullable(request.get(NETWORK_NAME)).map(JsonElement::getAsString).orElse(null);
        sortField = Optional.ofNullable(request.get(SORT_FIELD)).map(JsonElement::getAsString).orElse(null);
        String sortOrder = Optional.ofNullable(request.get(SORT_ORDER)).map(JsonElement::getAsString).orElse(null);
        sortOrderAsc = SortOrder.parse(sortOrder);
        take = Optional.ofNullable(request.get(TAKE)).map(JsonElement::getAsInt).orElse(null);
        skip = Optional.ofNullable(request.get(SKIP)).map(JsonElement::getAsInt).orElse(null);
        this.principal = principal;
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

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public boolean getSortOrderAsc() {
        return sortOrderAsc;
    }

    public void setSortOrderAsc(boolean sortOrderAsc) {
        this.sortOrderAsc = sortOrderAsc;
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

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(HivePrincipal principal) {
        this.principal = principal;
    }
}
