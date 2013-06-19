package com.devicehive.model;


import com.google.gson.annotations.SerializedName;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "device_notification")
public class DeviceNotification implements Serializable {

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @SerializedName("timestamp")
    @Column
    private Date timestamp;

    @SerializedName("notification")
    @Column
    @NotNull
    @NotBlank
    @Size(min = 1, max = 128)
    private String notification;

    @SerializedName("parameters")
    @Column
    private String parameters;

    @ManyToOne
    @JoinColumn
    @NotNull
    private Device device;

    public DeviceNotification() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
