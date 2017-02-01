package com.devicehive.dao.riak.model;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceEquipmentVO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RiakDeviceEquipment {

    private Long id;

    private String code;

    private Date timestamp;

    private JsonStringWrapper parameters;

    private String deviceGuid;

    private long entityVersion;

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public String getDevice() {
        return deviceGuid;
    }

    public void setDevice(String device) {
        this.deviceGuid = device;
    }

    //Riak indexes
    @RiakIndex(name = "device")
    public String getDeviceSi() {
        return deviceGuid;
    }

    public static List<DeviceEquipmentVO> convertToVo(List<RiakDeviceEquipment> equipment) {
        if (equipment == null) {
            return Collections.emptyList();
        }
        return equipment.stream().map(RiakDeviceEquipment::convertToVo).collect(Collectors.toList());
    }

    public static List<RiakDeviceEquipment> convertToEntity(List<DeviceEquipmentVO> equipment) {
        if (equipment == null) {
            return Collections.emptyList();
        }
        return equipment.stream().map(RiakDeviceEquipment::convertToEntity).collect(Collectors.toList());
    }

    public static DeviceEquipmentVO convertToVo(RiakDeviceEquipment equipment) {
        DeviceEquipmentVO vo = null;
        if (equipment != null) {
            vo = new DeviceEquipmentVO();
            vo.setCode(equipment.getCode());
            vo.setId(equipment.getId());
            vo.setParameters(equipment.getParameters());
            vo.setTimestamp(equipment.getTimestamp());
        }

        return vo;
    }

    public static RiakDeviceEquipment convertToEntity(DeviceEquipmentVO equipment) {
        RiakDeviceEquipment vo = null;
        if (equipment != null) {
            vo = new RiakDeviceEquipment();
            vo.setCode(equipment.getCode());
            vo.setId(equipment.getId());
            vo.setParameters(equipment.getParameters());
            vo.setTimestamp(equipment.getTimestamp());
        }

        return vo;
    }
}
