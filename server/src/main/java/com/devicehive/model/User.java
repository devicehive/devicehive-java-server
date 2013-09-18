package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity(name = "User")
@Table(name = "\"user\"")
@NamedQueries({
        @NamedQuery(name = "User.findByName", query = "select u from User u where u.login = :login and u.status <> 3"),
        @NamedQuery(name = "User.findByIdWithNetworks",
                query = "select u from User u left join fetch u.networks where u.id = :id"),
        @NamedQuery(name = "User.findActiveByName",
                query = "select u from User u where u.login = :login and u.status = 0"),
        @NamedQuery(name = "User.hasAccessToNetwork",
                query = "select count(distinct u) from User u join u.networks n " +
                        "where u = :user and n = :network"),
        @NamedQuery(name = "User.hasAccessToDevice",
                query = "select count(distinct n) from Network n join n.devices d join n.users u " +
                        "where u = :user and d = :device"),
        @NamedQuery(name = "User.hasAccessToDeviceByGuid", query = "select count(distinct n) " +
                "from Network n " +
                "join n.devices d " +
                "join n.users u " +
                "where u = :user and d.guid = :guid"),
        @NamedQuery(name = "User.getWithNetworksById",
                query = "select u from User u left join fetch u.networks where u.id = :id"),
        @NamedQuery(name = "User.getWithNetworks",
                query = "select u from User u left join fetch u.networks where u.login = :login"),

        @NamedQuery(name = "User.deleteById", query = "delete from User u where u.id = :id")
})
@Cacheable
public class User implements HiveEntity {

    private static final long serialVersionUID = -8980491502416082011L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("id")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Long id;

    @Column
    @SerializedName("login")
    @NotNull(message = "login field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of login should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private String login;

    @Column(name = "password_hash")
    @NotNull(message = "passwordHash field cannot be null.")
    @Size(min = 1, max = 64, message = "Field cannot be empty. The length of passwordHash should be 64 symbols.")
    private String passwordHash;

    @Column(name = "password_salt")
    @NotNull(message = "passwordSalt field cannot be null.")
    @Size(min = 1, max = 24, message = "Field cannot be empty. The length of passwordSalt should not be more than " +
            "24 symbols.")
    private String passwordSalt;

    @Column(name = "login_attempts")
    private Integer loginAttempts;

    @Column
    @SerializedName("role")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private UserRole role;

    @Column
    @SerializedName("status")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private UserStatus status;


    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
    @JsonPolicyDef({USER_PUBLISHED})
    private Set<Network> networks;

    @Column(name = "last_login")
    @SerializedName("lastLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Timestamp lastLogin;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    /**
     * @return true, if user is admin
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
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

    public Timestamp getLastLogin() {
        return ObjectUtils.cloneIfPossible(lastLogin);
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = ObjectUtils.cloneIfPossible(lastLogin);
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
     *
     * @param user      User that should be validated
     * @param validator Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(User user, Validator validator) {

        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);
        Set<String> result = new HashSet<>();

        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<User> cv : constraintViolations) {
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
            }
        }

        return result;

    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
