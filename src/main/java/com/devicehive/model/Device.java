package com.devicehive.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * TODO JavaDoc
 */
@Entity
public class Device {

    @SerializedName("id")
    @Id
    @GeneratedValue
    private UUID id;


    @SerializedName("key")
    @Column
    private String key;

    @SerializedName("name")
    @Column
    private String name;

    @SerializedName("status")
    @Column
    private String status;

    @SerializedName("data")
    private JsonElement data;

    @SerializedName("network")
    private Network network;

    @SerializedName("deviceClass")
    private DeviceClass deviceClass;

    @SerializedName("equipment")
    private List<Equipment> equipment;

    public Device() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public List<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<Equipment> equipment) {
        this.equipment = equipment;
    }
}
