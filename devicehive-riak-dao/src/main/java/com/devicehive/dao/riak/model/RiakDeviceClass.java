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
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RiakDeviceClass {

    private Long id;

    private String name;

    private Boolean isPermanent;

    private Integer offlineTimeout;

    private JsonStringWrapper data;

    private Set<RiakDeviceClassEquipment> equipment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @RiakIndex(name = "name")
    public void setNameRi(String nameRi) {
        this.name = nameRi;
    }

    @RiakIndex(name = "name")
    public String getNameRi() {
        return name;
    }

    public Boolean getPermanent() {
        return isPermanent;
    }

    public void setPermanent(Boolean permanent) {
        isPermanent = permanent;
    }

    public Integer getOfflineTimeout() {
        return offlineTimeout;
    }

    public void setOfflineTimeout(Integer offlineTimeout) {
        this.offlineTimeout = offlineTimeout;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Set<RiakDeviceClassEquipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<RiakDeviceClassEquipment> equipment) {
        this.equipment = equipment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RiakDeviceClass that = (RiakDeviceClass) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public static DeviceClassWithEquipmentVO convertDeviceClassWithEquipment(RiakDeviceClass deviceClass) {
        DeviceClassWithEquipmentVO vo = null;
        if (deviceClass != null) {
            vo = new DeviceClassWithEquipmentVO();
            vo.setName(deviceClass.getName());
            vo.setData(deviceClass.getData());
            vo.setId(deviceClass.getId());
            vo.setIsPermanent(deviceClass.getPermanent());

            if (deviceClass.getEquipment() != null) {
                Stream<DeviceClassEquipmentVO> eqVos = deviceClass.getEquipment().stream().map(RiakDeviceClassEquipment::convertDeviceClassEquipment);
                vo.setEquipment(eqVos.collect(Collectors.toSet()));
            }
        }
        return vo;
    }

    public static RiakDeviceClass convertDeviceClassVOToEntity(DeviceClassVO vo) {
        RiakDeviceClass en = null;
        if (vo != null) {
            en = new RiakDeviceClass();
            en.setId(vo.getId());
            en.setData(vo.getData());
            en.setName(vo.getName());
            en.setPermanent(vo.getIsPermanent());
        }
        return en;
    }

    public static RiakDeviceClass convertWithEquipmentToEntity(DeviceClassWithEquipmentVO vo) {
        RiakDeviceClass en = convertDeviceClassVOToEntity(vo);
        if (en != null) {
            if (vo.getEquipment() != null) {
                Set<RiakDeviceClassEquipment> equipmentSet = vo.getEquipment().stream().map(RiakDeviceClassEquipment::convertDeviceClassEquipmentVOToEntity).collect(Collectors.toSet());
                en.setEquipment(equipmentSet);
            } else {
                en.setEquipment(Collections.emptySet());
            }
        }
        return en;
    }


}
