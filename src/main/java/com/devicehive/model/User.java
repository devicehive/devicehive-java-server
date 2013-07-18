package com.devicehive.model;


import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity(name = "User")
@Table(name = "\"user\"")
@NamedQueries({
    @NamedQuery(name= "User.findByName", query = "select u from User u where u.login = :login and u.status <> 3"),
    @NamedQuery(name= "User.delete", query = "update User u set u.status = 3 where u.id = :id"),
    @NamedQuery(name= "User.findActiveByName", query = "select u from User u where u.login = :login and u.status = 0"),
    @NamedQuery(name= "User.hasAccessToNetwork", query = "select count(distinct u) from User u join u.networks n " +
            "where u = :user and n = :network"),
    @NamedQuery(name= "User.getWithNetworks", query = "select u from User u join fetch u.networks where u.id = :id")
})

public class User  implements Serializable {

    public static enum ROLE {Administrator, Client}
    public static enum STATUS {Active, LockedOut, Disabled, Deleted}


    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @SerializedName("id")
    private Long id;

    @Column
    @SerializedName("login")
    @NotNull(message = "login field cannot be null.")
    @Size(min = 1, max = 64, message = "Field cannot be empty. The length of login shouldn't be more than 64 symbols.")
    private String login;

    @Column(name = "password_hash")
    @NotNull(message = "passwordHash field cannot be null.")
    @Size(min = 1, max = 48, message = "Field cannot be empty. The length of passwordHash should be 48 symbols.")
    private String passwordHash;

    @Column(name = "password_salt")
    @NotNull(message = "passwordSalt field cannot be null.")
    @Size(min = 1, max = 24, message = "Field cannot be empty. The length of passwordSalt shouldn't be more than " +
            "24 symbols.")
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

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users", cascade = CascadeType.ALL)
    private Set<Network> networks;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;


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

    public Set<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<Network> networks) {
        this.networks = networks;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    /**
     * Validates user representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     * @param user
     * User that should be validated
     * @param validator
     * Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(User user, Validator validator) {
        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size()>0){
            for (ConstraintViolation<User> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }

    @Override
    public boolean equals(Object o){
        if(o instanceof User){
            return ((User)o).getId()==getId();
        }
        return false;
    }
}
