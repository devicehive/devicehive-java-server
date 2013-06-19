package com.devicehive.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * TODO JavaDoc
 */
@Entity(name = "device")
public class Device {

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @SerializedName("guid")
    @Column(columnDefinition = "uuid not null") // TODO That's postgres-specific
    private UUID guid;

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
    private String data;

    @SerializedName("network")
    @ManyToOne
    @JoinColumn(name = "network_id")
    private Network network;

    @SerializedName("deviceClass")
    @ManyToOne
    @JoinColumn(name = "device_class_id")
    private DeviceClass deviceClass;

    /*
    @SerializedName("equipment")
    @OneToMany
    private List<Equipment> equipment;
    */

    public Device() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
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
                  /*
    public List<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<Equipment> equipment) {
        this.equipment = equipment;
    }                                            */
}
