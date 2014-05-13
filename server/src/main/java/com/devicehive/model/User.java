package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USERS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_SUBMITTED;
import static com.devicehive.model.User.Queries.Names;
import static com.devicehive.model.User.Queries.Values;

@Entity(name = "User")
@Table(name = "\"user\"")
@NamedQueries({
        @NamedQuery(name = Names.FIND_BY_NAME, query = Values.FIND_BY_NAME),
        @NamedQuery(name = Names.HAS_ACCESS_TO_NETWORK, query = Values.HAS_ACCESS_TO_NETWORK),
        @NamedQuery(name = Names.HAS_ACCESS_TO_DEVICE, query = Values.HAS_ACCESS_TO_DEVICE),
        @NamedQuery(name = Names.GET_WITH_NETWORKS_BY_ID, query = Values.GET_WITH_NETWORKS_BY_ID),
        @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID)
})
@Cacheable
public class User implements HiveEntity {
    public static final String ID_COLUMN = "id";
    public static final String LOGIN_COLUMN = "login";
    public static final String ROLE_COLUMN = "role";
    public static final String STATUS_COLUMN = "status";
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

    public static class Queries {
        public static interface Names {
            static final String FIND_BY_NAME = "User.findByName";
            static final String HAS_ACCESS_TO_NETWORK = "User.hasAccessToNetwork";
            static final String HAS_ACCESS_TO_DEVICE = "User.hasAccessToDevice";
            static final String GET_WITH_NETWORKS_BY_ID = "User.getWithNetworksById";
            static final String DELETE_BY_ID = "User.deleteById";
        }

        static interface Values {
            static final String FIND_BY_NAME = "select u from User u where u.login = :login and u.status <> 3";
            static final String HAS_ACCESS_TO_NETWORK =
                    "select count(distinct u) from User u " +
                            "join u.networks n " +
                            "where u = :user and n = :network";
            static final String HAS_ACCESS_TO_DEVICE =
                    "select count(distinct n) from Network n " +
                            "join n.devices d " +
                            "join n.users u " +
                            "where u = :user and d = :device";
            static final String GET_WITH_NETWORKS_BY_ID =
                    "select u from User u left join fetch u.networks where u.id = :id";
            static final String DELETE_BY_ID = "delete from User u where u.id = :id";
        }

        public static interface Parameters {
            static final String USER = "user";
            static final String NETWORK = "network";
            static final String DEVICE = "device";
            static final String ID = "id";
            static final String LOGIN = "login";
        }
    }
}
