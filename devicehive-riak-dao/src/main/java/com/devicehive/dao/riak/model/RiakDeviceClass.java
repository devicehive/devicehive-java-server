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
import com.devicehive.vo.DeviceClassVO;

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

    public static DeviceClassVO convertDeviceClass(RiakDeviceClass deviceClass) {
        DeviceClassVO vo = null;
        if (deviceClass != null) {
            vo = new DeviceClassVO();
            vo.setName(deviceClass.getName());
            vo.setData(deviceClass.getData());
            vo.setId(deviceClass.getId());
            vo.setIsPermanent(deviceClass.getPermanent());
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



}
