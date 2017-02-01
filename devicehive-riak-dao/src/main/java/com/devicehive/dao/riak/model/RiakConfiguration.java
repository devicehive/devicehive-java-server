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


import com.devicehive.vo.ConfigurationVO;

public class RiakConfiguration {

    private String name;

    private long entityVersion;

    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }


    public static RiakConfiguration convert(ConfigurationVO vo) {
        if (vo != null) {
            RiakConfiguration result = new RiakConfiguration();
            result.setName(vo.getName());
            result.setValue(vo.getValue());
            result.setEntityVersion(vo.getEntityVersion());
            return result;
        } else {
            return null;
        }
    }

    public static ConfigurationVO convert(RiakConfiguration configuration) {
        if (configuration != null) {
            ConfigurationVO vo = new ConfigurationVO();
            vo.setName(configuration.getName());
            vo.setValue(configuration.getValue());
            vo.setEntityVersion(configuration.getEntityVersion());
            return vo;
        } else {
            return null;
        }
    }
}
