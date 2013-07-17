package com.devicehive.model.request;

import com.devicehive.model.User;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * @author Nikolay Loboda
 * @since 17.07.13
 */
public class UserInsert implements Serializable {

    @SerializedName("login")
    private String login;

    @SerializedName("role")
    private Integer role;

    @SerializedName("status")
    private Integer status;

    @SerializedName("password")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
