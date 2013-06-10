package com.devicehive.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO JavaDoc
 */
public class DeviceClass {

    @SerializedName("id")
    private Integer id;

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("isPermanent")
    private Boolean isPermanent;

    @SerializedName("offlineTimeout")
    private Integer offlineTimeout;

    @SerializedName("data")
    private JsonElement data;

    public DeviceClass() {

    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }
}
