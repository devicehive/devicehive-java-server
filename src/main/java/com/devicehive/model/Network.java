package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Entity
public class Network {

    @SerializedName("id")
    @Id
    @GeneratedValue
    private Integer id;

    @SerializedName("key")
    @Column
    private String key;

    @SerializedName("name")
    @Column
    private String name;

    @SerializedName("description")
    @Column
    private String description;

    @ManyToMany
    @JoinTable(name = "user_network")
    private List<User> users;

    public Network() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
