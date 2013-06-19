package com.devicehive.model;


import com.google.gson.annotations.SerializedName;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "\"user\"")
@NamedQueries({
    @NamedQuery(name= "User.findByName", query = "select u from User u where login = :login")
})

public class User {

    public static enum ROLE {Administrator, Client}
    public static enum STATUS {Active, LockedOut, Disabled, Deleted}


    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @SerializedName("id")
    private Long id;

    @Column
    @SerializedName("login")
    @NotNull
    @NotBlank
    @Size(min = 1, max = 64)
    private String login;


    @Column(name = "password_hash")
    @NotNull
    @NotBlank
    @Size(min = 1, max = 48)
    private String passwordHash;

    @Column(name = "password_salt")
    @NotNull
    @NotBlank
    @Size(min = 1, max = 24)
    private String passwordSalt;

    @Column(name = "login_attempts")
    private Integer loginAttempts;


    @Column
    @SerializedName("role")
    private Integer role;

    @Column
    @SerializedName("status")
    private Integer status;

    @Column(name = "last_login")
    @SerializedName("lastLogin")
    private Date lastLogin;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
    private List<Network> networks;


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

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(List<Network> networks) {
        this.networks = networks;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }
}
