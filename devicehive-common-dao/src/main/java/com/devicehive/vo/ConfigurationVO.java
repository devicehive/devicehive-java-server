package com.devicehive.vo;

import com.devicehive.model.HiveEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ConfigurationVO implements HiveEntity {

    @JsonProperty
    private String name;
    @Column
    @NotNull(message = "value field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of value should not be more than " +
            "128 symbols.")
    @JsonProperty
    private String value;
    @Version
    @Column(name = "entity_version")
    @JsonProperty
    private long entityVersion;

    public ConfigurationVO() {
    }

    public ConfigurationVO(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    @JsonIgnore
    public void setValue(long value) {
        this.value = Long.toString(value);
    }

    @JsonIgnore
    public void setValue(boolean value) {
        this.value = Boolean.toString(value);
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
}
