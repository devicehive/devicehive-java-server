package com.devicehive.dao.riak.model;

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
