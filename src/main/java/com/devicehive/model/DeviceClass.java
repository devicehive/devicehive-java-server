package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "device_class")
public class DeviceClass {

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @SerializedName("name")
    @Column
    private String name;

    @SerializedName("version")
    @Column
    private String version;

    @SerializedName("isPermanent")
    @Column(name = "is_permanent")
    private Boolean isPermanent;

    @SerializedName("offlineTimeout")
    @Column(name = "offline_timeout")
    private Integer offlineTimeout;

    @SerializedName("data")
    @Column
    private String data;

    public DeviceClass() {

    }

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
