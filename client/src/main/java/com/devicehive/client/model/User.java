package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import java.sql.Timestamp;
import java.util.Set;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class User implements HiveEntity {

    private static final long serialVersionUID = -8980491502416082011L;
    @JsonPolicyDef({COMMAND_TO_CLIENT, USER_PUBLISHED, COMMAND_TO_DEVICE, USERS_LISTED, USER_SUBMITTED})
    private Long id;
    @JsonPolicyDef({USER_PUBLISHED, USER_SUBMITTED, USERS_LISTED})
    private NullableWrapper<String> login;
    @JsonPolicyDef(USER_SUBMITTED)
    private NullableWrapper<String> password;
    @JsonPolicyDef({USER_PUBLISHED, USER_SUBMITTED, USERS_LISTED})
    private NullableWrapper<Integer> role;
    @JsonPolicyDef({USER_PUBLISHED, USER_SUBMITTED, USERS_LISTED})
    private NullableWrapper<Integer> status;
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Timestamp lastLogin;
    @JsonPolicyDef({USER_PUBLISHED})
    private Set<UserNetwork> networks;

    public User() {
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return NullableWrapper.value(login);
    }

    public void setLogin(String login) {
        this.login = NullableWrapper.create(login);
    }

    public void removeLogin() {
        this.login = null;
    }

    public String getPassword() {
        return NullableWrapper.value(password);
    }

    public void setPassword(String password) {
        this.password = NullableWrapper.create(password);
    }

    public void removePassword() {
        this.password = null;
    }

    public Integer getRole() {
        return NullableWrapper.value(role);
    }

    public void setRole(Integer role) {
        this.role = NullableWrapper.create(role);
    }

    public void removeRole() {
        this.role = null;
    }

    public Integer getStatus() {
        return NullableWrapper.value(status);
    }

    public void setStatus(Integer status) {
        this.status = NullableWrapper.create(status);
    }

    public void removeStatus() {
        this.status = null;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Set<UserNetwork> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<UserNetwork> networks) {
        this.networks = networks;
    }
}
