package com.devicehive.model.response;

import com.devicehive.model.User;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

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

    public User.ROLE getRoleEnum() {
        return role == 0 ? User.ROLE.Administrator : User.ROLE.Client;
    }

    public User.STATUS getStatusEnum() {
        switch (status) {
            case 0:
                return User.STATUS.Active;
            case 1:
                return User.STATUS.LockedOut;
            case 2:
                return User.STATUS.Disabled;
            case 3:
                return User.STATUS.Deleted;
            default:
                throw new IllegalArgumentException();
        }
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
