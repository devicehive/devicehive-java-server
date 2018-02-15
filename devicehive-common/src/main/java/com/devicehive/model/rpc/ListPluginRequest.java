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

public class ListPluginRequest extends Body {

    private String name;
    private String namePattern;
    private String topicName;
    private Integer status;
    private Long userId;
    private String sortField;
    private String sortOrderAsc;
    private Integer take;
    private Integer skip;
    private HivePrincipal principal;

    public ListPluginRequest() {
        super(Action.LIST_PLUGIN_REQUEST);
    }

    public ListPluginRequest(String name, String namePattern, String topicName, Integer status, Long userId,
                             String sortField, String sortOrderAsc, Integer take, Integer skip,
                             HivePrincipal principal) {
        super(Action.LIST_PLUGIN_REQUEST);

        this.name = name;
        this.namePattern = namePattern;
        this.topicName = topicName;
        this.status = status;
        this.userId = userId;
        this.sortField = sortField;
        this.sortOrderAsc = sortOrderAsc;
        this.take = take;
        this.skip = skip;
        this.principal = principal;
    }

    public static ListPluginRequest createListPluginRequest(JsonObject request, HivePrincipal principal) {
        ListPluginRequest listPluginRequest = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, ListPluginRequest.class);

        listPluginRequest.setTake(Optional.ofNullable(listPluginRequest.getTake()).orElse(DEFAULT_TAKE));
        listPluginRequest.setSkip(Optional.ofNullable(listPluginRequest.getSkip()).orElse(DEFAULT_SKIP));

        listPluginRequest.setPrincipal(principal);

        return listPluginRequest;
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

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrderAsc() {
        return sortOrderAsc;
    }

    public void setSortOrderAsc(String sortOrderAsc) {
        this.sortOrderAsc = sortOrderAsc;
    }

    public boolean isSortOrderAsc() {
        return SortOrder.parse(sortOrderAsc);
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
