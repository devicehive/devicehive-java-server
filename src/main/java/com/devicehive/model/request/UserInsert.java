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
    private UserRole role;

    @SerializedName("status")
    private UserStatus status;

    @SerializedName("password")
    private String password;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
