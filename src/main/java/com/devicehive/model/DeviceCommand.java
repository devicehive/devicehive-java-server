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
@Table(name = "device_command")
public class DeviceCommand implements Serializable{

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;

    @SerializedName("timestamp")
    @Column
    public Date timestamp;

    @SerializedName("user")
    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @SerializedName("device")
    @ManyToOne
    @JoinColumn(name = "device_id")
    @NotNull
    public Device device;

    @SerializedName("command")
    @Column
    @NotNull
    @NotBlank
    @Size(min = 1, max = 128)
    public String command;

    @SerializedName("parameters")
    @Column
    public String parameters;

    @SerializedName("lifetime")
    @Column
    public Integer lifetime;

    @SerializedName("flags")
    @Column
    public Integer flags;

    @SerializedName("status")
    @Column
    public String status;

    @SerializedName("result")
    @Column
    public String result;

    public DeviceCommand() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
