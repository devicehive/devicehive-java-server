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

import com.devicehive.model.enums.SortOrder;
import com.devicehive.shim.api.Body;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Optional;

import static com.devicehive.configuration.Constants.DEFAULT_SKIP;
import static com.devicehive.configuration.Constants.DEFAULT_TAKE;

public class ListNotificationRequest extends Body {

    private String deviceId;
    private Date start;
    private Date end;
    private String notification;
    private String sortField;
    private String sortOrder;
    private Integer take;
    private Integer skip;

    public ListNotificationRequest() {
        super(Action.LIST_NOTIFICATION_REQUEST.name());
    }

    public static ListNotificationRequest createListNotificationRequest(JsonObject request) {
        ListNotificationRequest listNotificationRequest = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, ListNotificationRequest.class);
        listNotificationRequest.setTake(Optional.ofNullable(listNotificationRequest.getTake()).orElse(DEFAULT_TAKE));
        listNotificationRequest.setSkip(Optional.ofNullable(listNotificationRequest.getSkip()).orElse(DEFAULT_SKIP));

        return listNotificationRequest;
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

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
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
