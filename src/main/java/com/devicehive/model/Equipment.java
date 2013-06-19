package com.devicehive.model;

import com.google.gson.annotations.SerializedName;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "equipment")
public class Equipment {
    @SerializedName("id")

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @SerializedName("name")
    @Column
    @NotNull
    @NotBlank
    @Size(min = 1, max = 128)
    private String name;

    @SerializedName("code")
    @Column
    @NotNull
    @NotBlank
    @Size(min = 1, max = 128)
    private String code;

    @SerializedName("type")
    @Column
    @NotBlank
    @NotNull
    @Size(min = 1, max = 128)
    private String type;

    @SerializedName("data")
    @Column
    private String data;

    @ManyToOne
    @JoinColumn(name = "device_class_id")
    @NotNull
    private DeviceClass deviceClass;

    public Equipment() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }
}
