package com.devicehive.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Set;

/**
 * @author Nikolay Loboda <madlooser@gmail.com>
 * @since 7/18/13 1:34 AM
 */
public class DetailedUserResponse {

    @SerializedName("id")
    private long id;

    @SerializedName("login")
    private String login;

    @SerializedName("role")
    private int role;

    @SerializedName("status")
    private int status;


    @SerializedName("lastLogin")
    private Date lastLogin;

    @SerializedName("networks")
    private Set<SimpleNetworkResponse> networks;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Set<SimpleNetworkResponse> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<SimpleNetworkResponse> networks) {
        this.networks = networks;
    }
}
