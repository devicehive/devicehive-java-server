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

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.enums.SortOrder;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Optional;

import static com.devicehive.configuration.Constants.DEFAULT_SKIP;
import static com.devicehive.configuration.Constants.DEFAULT_TAKE;

public class ListCommandRequest extends Body {

    private String deviceId;
    private Date start;
    private Date end;
    private String command;
    private String status;
    private String sortField;
    private String sortOrder;
    private Integer take;
    private Integer skip;

    public ListCommandRequest() {
        super(Action.LIST_COMMAND_REQUEST);
    }

    public static ListCommandRequest createListCommandRequest(JsonObject request) {
        ListCommandRequest listCommandRequest = new GsonBuilder()
                .registerTypeAdapter(Date.class, new TimestampAdapter())
                .excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, ListCommandRequest.class);
        listCommandRequest.setTake(Optional.ofNullable(listCommandRequest.getTake()).orElse(DEFAULT_TAKE));
        listCommandRequest.setSkip(Optional.ofNullable(listCommandRequest.getSkip()).orElse(DEFAULT_SKIP));
        
        return listCommandRequest;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isSortOrderAsc() {
        return SortOrder.parse(sortOrder);
    }

}
