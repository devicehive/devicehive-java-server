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
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static com.devicehive.configuration.Constants.DEFAULT_SKIP;
import static com.devicehive.configuration.Constants.DEFAULT_TAKE;
import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.NOTIFICATION;

//TODO [rafa] That object to be split into two different objects. The first would contain id+deviceId, the second the rest of the fields.
public class NotificationSearchRequest extends Body {

    private Long id;
    private Set<String> deviceIds;
    private Set<String> names;
    @SerializedName("start")
    private Date timestampStart;
    @SerializedName("end")
    private Date timestampEnd;
    private String status;
    private String sortField;
    private String sortOrder;
    private Integer take;
    private Integer skip;

    public NotificationSearchRequest() {
        super(Action.NOTIFICATION_SEARCH_REQUEST);
    }

    public static NotificationSearchRequest createNotificationSearchRequest(JsonObject request) {
        NotificationSearchRequest notificationSearchRequest = new GsonBuilder()
                .registerTypeAdapter(Date.class, new TimestampAdapter())
                .excludeFieldsWithModifiers(Modifier.PROTECTED)
                .create()
                .fromJson(request, NotificationSearchRequest.class);
        notificationSearchRequest.setTake(Optional.ofNullable(notificationSearchRequest.getTake()).orElse(DEFAULT_TAKE));
        notificationSearchRequest.setSkip(Optional.ofNullable(notificationSearchRequest.getSkip()).orElse(DEFAULT_SKIP));

        if (CollectionUtils.isEmpty(notificationSearchRequest.getDeviceIds())) {
            Optional.ofNullable(request.get(DEVICE_ID)).map(JsonElement::getAsString).ifPresent(deviceId -> {
                notificationSearchRequest.setDeviceIds(Collections.singleton(deviceId));
            });
        }

        if (CollectionUtils.isEmpty(notificationSearchRequest.getNames())) {
            Optional.ofNullable(request.get(NOTIFICATION)).map(JsonElement::getAsString).ifPresent(notification -> {
                notificationSearchRequest.setNames(Collections.singleton(notification));
            });
        }

        return notificationSearchRequest;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<String> getDeviceIds() {
        return deviceIds;
    }

    public String getDeviceId() {
        return Optional.ofNullable(deviceIds)
                .map(ids -> ids.stream().findFirst().orElse(null))
                .orElse(null);
    }

    public void setDeviceIds(Set<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
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
}
