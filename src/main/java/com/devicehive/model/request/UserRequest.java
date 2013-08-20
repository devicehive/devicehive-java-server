package com.devicehive.model.request;

import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.UserRole;
import com.devicehive.model.UserStatus;
import com.google.gson.annotations.SerializedName;

/**
 * @author Nikolay Loboda
 * @since 17.07.13
 */
public class UserRequest implements HiveEntity {

    private static final long serialVersionUID = -8353201743020153250L;
    @SerializedName("login")
    private NullableWrapper<String> login;

    @SerializedName("role")
    private NullableWrapper<Integer> role;

    @SerializedName("status")
    private NullableWrapper<Integer> status;

    @SerializedName("password")
    private NullableWrapper<String> password;

    public NullableWrapper<String> getLogin() {
        return login;
    }

    public void setLogin(NullableWrapper<String> login) {
        this.login = login;
    }

    public NullableWrapper<Integer> getRole() {
        return role;
    }

    public void setRole(NullableWrapper<Integer> role) {
        this.role = role;
    }

    public NullableWrapper<Integer> getStatus() {
        return status;
    }

    public void setStatus(NullableWrapper<Integer> status) {
        this.status = status;
    }

    public NullableWrapper<String> getPassword() {
        return password;
    }

    public void setPassword(NullableWrapper<String> password) {
        this.password = password;
    }

    public UserRole getRoleEnum() {
        if (role == null) {
            return null;
        }
        Integer r = role.getValue();
        if (r == null) {
            return null;
        }
        return UserRole.values()[r];
    }

    public UserStatus getStatusEnum() {
        if (status == null) {
            return null;
        }
        Integer s = status.getValue();
        if (s == null) {
            return null;
        }
        return UserStatus.values()[s];
    }
}
