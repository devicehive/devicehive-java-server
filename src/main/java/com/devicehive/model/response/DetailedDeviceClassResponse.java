package com.devicehive.model.response;

import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

import java.util.Set;

/**
 * @author Nikolay Loboda
 * @since 7/21/13 10:14 PM
 */
public class DetailedDeviceClassResponse {

    @SerializedName("id")
    private Long id;

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

    @SerializedName("equipment")
    private Set<SimpleEquipmentResponse> equipment;

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

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Set<SimpleEquipmentResponse> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<SimpleEquipmentResponse> equipment) {
        this.equipment = equipment;
    }
}
