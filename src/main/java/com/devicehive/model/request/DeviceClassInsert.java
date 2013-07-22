package com.devicehive.model.request;

import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

/**
 * @author Nikolay Loboda
 * @since 7/21/13 10:40 PM
 */
public class DeviceClassInsert {

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("isPermanent")
    private Boolean isPermanent;

    @SerializedName("offlineTimeout")
    private Integer offlineTimeout;

    @SerializedName("data")
    private JsonStringWrapper data;

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

    public Boolean getIsPermanent() {
        return isPermanent;
    }

    public void setIsPermanent(Boolean permanent) {
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
}
