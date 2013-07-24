package com.devicehive.model.response;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

/**
 * @author Nikolay Loboda
 * @since 18.07.13
 */
public class SimpleUserResponse implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("login")
    private String login;

    @SerializedName("role")
    private Integer role;

    @SerializedName("status")
    private Integer status;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
