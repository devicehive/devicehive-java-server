package com.devicehive.model;


import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class User {

    @Id
    @GeneratedValue
    @SerializedName("id")
    private Integer id;

    @Column
    @SerializedName("login")
    private String login;

    @Column
    @SerializedName("role")
    private Integer role;

    @Column
    @SerializedName("status")
    private Integer status;

    @Column
    @SerializedName("lastLogin")
    private Date lastLogin;

    @ManyToMany
    @JoinTable(name = "user_network")
    private List<Network> networks;


    public User() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
}
