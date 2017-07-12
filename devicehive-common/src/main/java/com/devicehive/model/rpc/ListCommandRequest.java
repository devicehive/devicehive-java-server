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

import com.devicehive.model.converters.TimestampQueryParamParser;
import com.devicehive.model.enums.SortOrder;
import com.devicehive.shim.api.Body;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.devicehive.configuration.Constants.COMMAND;
import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.END_TIMESTAMP;
import static com.devicehive.configuration.Constants.SKIP;
import static com.devicehive.configuration.Constants.SORT_FIELD;
import static com.devicehive.configuration.Constants.SORT_ORDER;
import static com.devicehive.configuration.Constants.START_TIMESTAMP;
import static com.devicehive.configuration.Constants.STATUS;
import static com.devicehive.configuration.Constants.TAKE;

public class ListCommandRequest extends Body {

    private String deviceId;
    private Date timestampStart;
    private Date timestampEnd;
    private String command;
    private String status;
    private String sortField;
    private String sortOrder;
    private Integer take;
    private Integer skip;

    public ListCommandRequest() {
        super(Action.LIST_COMMAND_REQUEST.name());
    }

    public ListCommandRequest(JsonObject request) {
        super(Action.LIST_COMMAND_REQUEST.name());
        deviceId = Optional.ofNullable(request.get(DEVICE_ID)).map(JsonElement::getAsString).orElse(null);
        String startTs = Optional.ofNullable(request.get(START_TIMESTAMP)).map(JsonElement::getAsString).orElse(null);
        timestampStart = Objects.nonNull(startTs) ? TimestampQueryParamParser.parse(startTs): null;
        String endTs = Optional.ofNullable(request.get(END_TIMESTAMP)).map(JsonElement::getAsString).orElse(null);
        timestampEnd = Objects.nonNull(startTs) ? TimestampQueryParamParser.parse(startTs): null;
        command = Optional.ofNullable(request.get(COMMAND)).map(JsonElement::getAsString).orElse(null);
        status = Optional.ofNullable(request.get(STATUS)).map(JsonElement::getAsString).orElse(null);
        sortField = Optional.ofNullable(request.get(SORT_FIELD)).map(JsonElement::getAsString).orElse(null);
        sortOrder = Optional.ofNullable(request.get(SORT_ORDER)).map(JsonElement::getAsString).orElse(null);
        take = Optional.ofNullable(request.get(TAKE)).map(JsonElement::getAsInt).orElse(100);
        skip = Optional.ofNullable(request.get(SKIP)).map(JsonElement::getAsInt).orElse(0);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Date getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(Date timestampStart) {
        this.timestampStart = timestampStart;
    }

    public Date getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(Date timestampEnd) {
        this.timestampEnd = timestampEnd;
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
