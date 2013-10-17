package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
@Entity
@NamedQueries({
        @NamedQuery(name = "AccessKey.getByUserId", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user u where u.id = :userId"),
        @NamedQuery(name = "AccessKey.getById", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user u " +
                "where u.id = :userId and ak.id = :accessKeyId"),
        @NamedQuery(name = "AccessKey.getByKey", query = "select ak from AccessKey ak left join fetch ak.permissions join fetch ak.user where" +
                " ak.key = :someKey"),
        @NamedQuery(name = "AccessKey.deleteByIdAndsUser", query = "delete from AccessKey ak " +
                "where ak.user.id = :userId and ak.id = :accessKeyId"),
        @NamedQuery(name = "AccessKey.deleteById", query = "delete from AccessKey ak where ak.id = :accessKeyId")
})
@Table(name = "access_key")
@Cacheable
public class AccessKey implements HiveEntity {

    private static final long serialVersionUID = 7609754275823483733L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_SUBMITTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
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
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_SUBMITTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    @NotNull(message = "User column cannot be null")
    private User user;

    @Column(name = "expiration_date")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private Timestamp expirationDate;

    @OneToMany(mappedBy = "accessKey")
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

    public Timestamp getExpirationDate() {
        return ObjectUtils.cloneIfPossible(expirationDate);
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = ObjectUtils.cloneIfPossible(expirationDate);
    }
}
