package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.*;

/**
 * TODO JavaDoc
 */
@Entity
public class Equipment {
    @SerializedName("id")

    @Id
    @GeneratedValue
    private Integer id;

    @SerializedName("name")
    @Column
    private String name;

    @SerializedName("code")
    @Column
    private String code;

    @SerializedName("type")
    @Column
    private String type;

    @SerializedName("data")
    @Column
    private String data;

    @ManyToOne
    @JoinColumn
    private Device device;

    public Equipment() {
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
}
