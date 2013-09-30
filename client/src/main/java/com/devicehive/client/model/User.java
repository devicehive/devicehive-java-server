package com.devicehive.client.model;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.sql.Timestamp;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class User implements HiveEntity {

    private static final long serialVersionUID = -8980491502416082011L;
    @JsonPolicyDef({COMMAND_TO_CLIENT, USER_PUBLISHED, COMMAND_TO_DEVICE, USERS_LISTED, USER_SUBMITTED})
    private Long id;
    @JsonPolicyDef({USER_PUBLISHED, USER_SUBMITTED, USERS_LISTED})
    private String login;
    @JsonPolicyDef(USER_SUBMITTED)
    private String password;
    @JsonPolicyDef({USER_PUBLISHED,USER_SUBMITTED, USERS_LISTED})
    private Integer role;
    @JsonPolicyDef({USER_PUBLISHED,USER_SUBMITTED, USERS_LISTED})
    private Integer status;
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
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
