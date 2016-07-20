package com.devicehive.model;

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.enums.AccessKeyType;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@NamedQueries({
        @NamedQuery(name = "AccessKey.getByUserId", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user u where u.id = :userId"),
        @NamedQuery(name = "AccessKey.getById", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user u where u.id = :userId and ak.id = :accessKeyId"),
        @NamedQuery(name = "AccessKey.getByIdSimple", query = "select ak from AccessKey ak left join fetch ak.permissions join ak.user u where u.id = :userId and ak.id = :accessKeyId"),
        @NamedQuery(name = "AccessKey.getByKey", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user where ak.key = :someKey"),
        @NamedQuery(name = "AccessKey.getByUserAndLabel", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user u where u.id = :userId and ak.label = :label"),
        @NamedQuery(name = "AccessKey.deleteByIdAndUser", query = "delete from AccessKey ak where ak.user.id = :userId and ak.id = :accessKeyId"),
        @NamedQuery(name = "AccessKey.deleteById", query = "delete from AccessKey ak where ak.id = :accessKeyId"),
        @NamedQuery(name = "AccessKey.deleteOlderThan", query = "delete from AccessKey ak where ak.expirationDate < :expirationDate")
})
@Table(name = "access_key")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AccessKey implements HiveEntity {

    public static final String PERMISSIONS_COLUMN = "permissions";
    private static final long serialVersionUID = 7609754275823483733L;

    public AccessKey() {
        this.type = AccessKeyType.DEFAULT;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({ACCESS_KEY_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_LISTED_ADMIN,
            OAUTH_GRANT_LISTED, ACCESS_KEY_SUBMITTED})
    private Long id;

    @Column
    @NotNull(message = "Label column cannot be null")
    @Size(min = 1, max = 64,
            message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private String label;

    @Column
    @NotNull(message = "Key column cannot be null")
    @Size(min = 1, max = 48,
            message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_SUBMITTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_LISTED_ADMIN,
            OAUTH_GRANT_LISTED})
    private String key;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", updatable = false)
    @NotNull(message = "User column cannot be null")
    private User user;

    @Column(name = "expiration_date")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    @Temporal(TemporalType.TIMESTAMP)
    private Date expirationDate;

    @SerializedName("type")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private AccessKeyType type;

    @OneToMany(mappedBy = "accessKey", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @NotNull
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private Set<AccessKeyPermission> permissions;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    @Transient
    private long userId;

    public Set<AccessKeyPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccessKeyPermission> permissions) {
        this.permissions = permissions;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : -1;
    }

    public Date getExpirationDate() {
        return ObjectUtils.cloneIfPossible(expirationDate);
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = ObjectUtils.cloneIfPossible(expirationDate);
    }

    public AccessKeyType getType() {
        return type;
    }

    public void setType(AccessKeyType type) {
        this.type = type;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @RiakIndex(name = "userId")
    public long getUserIdSi() {
        return userId;
    }

    @RiakIndex(name = "userId")
    public void setUserIdSi(long userId) {
        this.userId = userId;
    }

    @RiakIndex(name = "key")
    public String getKeySi() {
        return key;
    }

    @RiakIndex(name = "key")
    public void setKeySi(String key) {
        this.key = key;
    }

    @RiakIndex(name = "expirationDate")
    public Long getExpirationDateSi() {
        return expirationDate != null ? expirationDate.getTime() : -1;
    }

    @RiakIndex(name = "expirationDate")
    public void setExpirationDateSi(Long expirationDate) {
        this.expirationDate = new Date(expirationDate);
    }
}
