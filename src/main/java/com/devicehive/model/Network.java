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
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

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
    @JoinTable(name = "user_network", joinColumns = {@JoinColumn(name = "user_id")}, inverseJoinColumns = {@JoinColumn(name = "network_id")})
    private List<User> users;

    public Network() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
