package com.devicehive.model;


import com.google.gson.annotations.SerializedName;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.DefaultValue;

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
                  @NamedQuery(name = Names.FIND_BY_GOOGLE_NAME, query = Values.FIND_BY_GOOGLE_NAME),
                  @NamedQuery(name = Names.FIND_BY_FACEBOOK_NAME, query = Values.FIND_BY_FACEBOOK_NAME),
                  @NamedQuery(name = Names.FIND_BY_GITHUB_NAME, query = Values.FIND_BY_GITHUB_NAME),
                  @NamedQuery(name = Names.FIND_BY_IDENTITY_NAME, query = Values.FIND_BY_IDENTITY_NAME),
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
    private String passwordHash;
    @Column(name = "password_salt")
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
    @Column(name="google_login")
    @SerializedName("googleLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String googleLogin;
    @Column(name="facebook_login")
    @SerializedName("facebookLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String facebookLogin;
    @Column(name="github_login")
    @SerializedName("githubLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String githubLogin;
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

    public String getGoogleLogin() {
        return googleLogin;
    }

    public void setGoogleLogin(String googleLogin) {
        this.googleLogin = StringUtils.trim(googleLogin);
    }

    public String getFacebookLogin() {
        return facebookLogin;
    }

    public void setFacebookLogin(String facebookLogin) {
        this.facebookLogin = StringUtils.trim(facebookLogin);
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(String githubLogin) {
        this.githubLogin = StringUtils.trim(githubLogin);
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
            static final String FIND_BY_GOOGLE_NAME = "User.findByGoogleName";
            static final String FIND_BY_FACEBOOK_NAME = "User.findByFacebookName";
            static final String FIND_BY_GITHUB_NAME = "User.findByGithubName";
            static final String FIND_BY_IDENTITY_NAME = "User.findByIdentityName";
            static final String HAS_ACCESS_TO_NETWORK = "User.hasAccessToNetwork";
            static final String HAS_ACCESS_TO_DEVICE = "User.hasAccessToDevice";
            static final String GET_WITH_NETWORKS_BY_ID = "User.getWithNetworksById";
            static final String DELETE_BY_ID = "User.deleteById";
        }

        static interface Values {

            static final String FIND_BY_NAME = "select u from User u where u.login = :login and u.status <> 3";
            static final String FIND_BY_GOOGLE_NAME = "select u from User u where upper(u.googleLogin) = upper(:login) and u.status <> 3";
            static final String FIND_BY_FACEBOOK_NAME = "select u from User u where upper(u.facebookLogin) = upper(:login) and u.status <> 3";
            static final String FIND_BY_GITHUB_NAME = "select u from User u where upper(u.githubLogin) = upper(:login) and u.status <> 3";
            static final String FIND_BY_IDENTITY_NAME = "select u from User u where u.login<> :login and (u.googleLogin = :googleLogin " +
                    "or u.facebookLogin = :facebookLogin or u.githubLogin = :githubLogin) and u.status <> 3";
            static final String HAS_ACCESS_TO_NETWORK =
                "select count(distinct u) from User u " +
                "join u.networks n " +
                "where u = :user and n = :network";
            static final String HAS_ACCESS_TO_DEVICE =
                "select count(distinct n) from Network n " +
                "join n.devices d " +
                "join n.users u " +
                "where u = :user and d.guid = :guid";
            static final String GET_WITH_NETWORKS_BY_ID =
                "select u from User u left join fetch u.networks where u.id = :id";
            static final String DELETE_BY_ID = "delete from User u where u.id = :id";
        }

        public static interface Parameters {

            static final String USER = "user";
            static final String NETWORK = "network";
            static final String DEVICE = "device";
            static final String ID = "id";
            static final String GUID = "guid";
            static final String LOGIN = "login";
            static final String GOOGLE_LOGIN = "googleLogin";
            static final String FACEBOOK_LOGIN = "facebookLogin";
            static final String GITHUB_LOGIN = "githubLogin";
        }
    }
}
