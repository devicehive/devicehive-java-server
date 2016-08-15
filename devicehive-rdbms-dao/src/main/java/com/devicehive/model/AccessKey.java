package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;
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

    @OneToMany(mappedBy = "accessKey", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @NotNull
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private Set<AccessKeyPermission> permissions;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

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

    public static AccessKey convert(AccessKeyVO accessKey) {
        AccessKey result = null;
        if (accessKey != null) {
            result = new AccessKey();
            result.setId(accessKey.getId());
            result.setLabel(accessKey.getLabel());
            result.setKey(accessKey.getKey());
            User user = User.convertToEntity(accessKey.getUser());
            result.setUser(user);
            result.setExpirationDate(accessKey.getExpirationDate());
            result.setType(accessKey.getType());
            Set<AccessKeyPermission> permissions = AccessKeyPermission.convertToEntity(accessKey.getPermissions());
            for (AccessKeyPermission permission : permissions) {
                permission.setAccessKey(result);
            }
            result.setPermissions(permissions);
            result.setEntityVersion(accessKey.getEntityVersion());
        }
        return result;
    }

    public static AccessKeyVO convert(AccessKey accessKey) {
        if (accessKey != null) {
            AccessKeyVO result = new AccessKeyVO();
            result.setId(accessKey.getId());
            result.setLabel(accessKey.getLabel());
            result.setKey(accessKey.getKey());
            UserVO user = User.convertToVo(accessKey.getUser());
            result.setUser(user);
            result.setExpirationDate(accessKey.getExpirationDate());
            result.setType(accessKey.getType());
            Set<AccessKeyPermissionVO> permissions = AccessKeyPermission.converttoVO(accessKey.getPermissions());
            result.setPermissions(permissions);
            result.setEntityVersion(accessKey.getEntityVersion());
            return result;
        } else {
            return null;
        }
    }
}
