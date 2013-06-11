package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO JavaDoc
 */
@Entity
public class DeviceClass {

    @SerializedName("id")
    @Id
    @GeneratedValue
    private Integer id;

    @SerializedName("name")
    @Column
    private String name;

    @SerializedName("version")
    @Column
    private String version;

    @SerializedName("isPermanent")
    @Column
    private Boolean isPermanent;

    @SerializedName("offlineTimeout")
    @Column
    private Integer offlineTimeout;

    @SerializedName("data")
    @Column
    private String data;


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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
