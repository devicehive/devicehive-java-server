package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity(name = "User")
@Table(name = "\"user\"")
@NamedQueries({
        @NamedQuery(name = "User.findByName", query = "select u from User u where u.login = :login and u.status <> 3"),
        @NamedQuery(name = "User.findByGoogleName", query = "select u from User u where upper(u.googleLogin) = upper(:login) and u.status <> 3"),
        @NamedQuery(name = "User.findByFacebookName", query = "select u from User u where upper(u.facebookLogin) = upper(:login) and u.status <> 3"),
        @NamedQuery(name = "User.findByGithubName", query = "select u from User u where upper(u.githubLogin) = upper(:login) and u.status <> 3"),
        @NamedQuery(name = "User.findByIdentityName", query = "select u from User u where u.login<> :login and (u.googleLogin = :googleLogin or u.facebookLogin = :facebookLogin or u.githubLogin = :githubLogin) and u.status <> 3"),
        @NamedQuery(name = "User.hasAccessToNetwork", query = "select count(distinct u) from User u join u.networks n where u = :user and n = :network"),
        @NamedQuery(name = "User.hasAccessToDevice", query = "select count(distinct n) from Network n join n.devices d join n.users u where u = :user and d.guid = :guid"),
        @NamedQuery(name = "User.getWithNetworksById", query = "select u from User u left join fetch u.networks where u.id = :id"),
        @NamedQuery(name = "User.deleteById", query = "delete from User u where u.id = :id")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;
    @Column(name="google_login")
    @SerializedName("googleLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String googleLogin;
    @Column(name = "facebook_login")
    @SerializedName("facebookLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String facebookLogin;
    @Column(name = "github_login")
    @SerializedName("githubLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String githubLogin;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;
    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
    })
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private JsonStringWrapper data;

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

    public Date getLastLogin() {
        return ObjectUtils.cloneIfPossible(lastLogin);
    }

    public void setLastLogin(Date lastLogin) {
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

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
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
