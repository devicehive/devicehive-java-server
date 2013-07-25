package com.devicehive.model.request;

import java.io.Serializable;

import com.devicehive.model.UserRole;
import com.devicehive.model.UserStatus;
import com.google.gson.annotations.SerializedName;

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

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Integer getRole() {
        return role;
    }

    public UserRole getRoleEnum() {
        return UserRole.values()[role];
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public UserStatus getStatusEnum() {
        return UserStatus.values()[status];
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
