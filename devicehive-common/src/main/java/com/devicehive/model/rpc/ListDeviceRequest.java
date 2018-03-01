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

public class ListDeviceRequest extends Body {

    private String name;
    private String namePattern;
    private Long networkId;
    private String networkName;
    private String sortField;
    private String sortOrder;
    private Integer take;
    private Integer skip;
    private HivePrincipal principal;

    public ListDeviceRequest() {
        super(Action.LIST_DEVICE_REQUEST);
    }

    public ListDeviceRequest(Long networkId) {
        super(Action.LIST_DEVICE_REQUEST);
        this.networkId = networkId;
    }

    public ListDeviceRequest(String sortOrder, HivePrincipal principal) {
        super(Action.LIST_DEVICE_REQUEST);
        this.sortOrder = sortOrder;
        this.principal = principal;
    }

    public static ListDeviceRequest createListDeviceRequest(JsonObject request, HivePrincipal principal) {
        ListDeviceRequest listDeviceRequest = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, ListDeviceRequest.class);
        listDeviceRequest.setTake(Optional.ofNullable(listDeviceRequest.getTake()).orElse(DEFAULT_TAKE));
        listDeviceRequest.setSkip(Optional.ofNullable(listDeviceRequest.getSkip()).orElse(DEFAULT_SKIP));
        
        listDeviceRequest.setPrincipal(principal);
                
        return listDeviceRequest;
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

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSortOrderAsc() {
        return SortOrder.parse(sortOrder);
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
