package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.enums.AccessKeyType;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.model.AccessKey.Queries.Names;
import static com.devicehive.model.AccessKey.Queries.Values;

@Entity
@NamedQueries({
                  @NamedQuery(name = Names.GET_BY_USER_ID, query = Values.GET_BY_USER_ID),
                  @NamedQuery(name = Names.GET_BY_ID, query = Values.GET_BY_ID),
                  @NamedQuery(name = Names.GET_BY_ID_SIMPLE, query = Values.GET_BY_ID_SIMPLE),
                  @NamedQuery(name = Names.GET_BY_KEY, query = Values.GET_BY_KEY),
                  @NamedQuery(name = Names.GET_BY_USER_AND_LABEL, query = Values.GET_BY_USER_AND_LABEL),
                  @NamedQuery(name = Names.DELETE_BY_ID_AND_USER, query = Values.DELETE_BY_ID_AND_USER),
                  @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID),
                  @NamedQuery(name = Names.DELETE_OLDER_THAN, query = Values.DELETE_OLDER_THAN)
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
    private Timestamp expirationDate;

    @SerializedName("type")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private AccessKeyType type;

    @OneToMany(mappedBy = "accessKey", fetch = FetchType.EAGER)
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

    public AccessKeyType getType() {
        return type;
    }

    public void setType(AccessKeyType type) {
        this.type = type;
    }

    public static class Queries {

        public static interface Names {

            static final String GET_BY_USER_ID = "AccessKey.getByUserId";
            static final String GET_BY_ID = "AccessKey.getById";
            static final String GET_BY_ID_SIMPLE = "AccessKey.getByIdSimple";
            static final String GET_BY_KEY = "AccessKey.getByKey";
            static final String GET_BY_USER_AND_LABEL = "AccessKey.getByUserAndLabel";
            static final String DELETE_BY_ID_AND_USER = "AccessKey.deleteByIdAndUser";
            static final String DELETE_BY_ID = "AccessKey.deleteById";
            static final String DELETE_OLDER_THAN = "AccessKey.deleteOlderThan";
        }

        static interface Values {

            static final String GET_BY_USER_ID =
                "select ak from AccessKey ak " +
                "left join fetch ak.permissions " +
                "join fetch ak.user u " +
                "where u.id = :userId";
            static final String GET_BY_ID =
                "select ak from AccessKey ak " +
                "left join fetch ak.permissions " +
                "join fetch ak.user u " +
                "where u.id = :userId and ak.id = :accessKeyId";
            static final String GET_BY_ID_SIMPLE =
                "select ak from AccessKey ak " +
                "left join fetch ak.permissions " +
                "join ak.user u " +
                "where u.id = :userId and ak.id = :accessKeyId";
            static final String GET_BY_KEY =
                "select ak from AccessKey ak " +
                "left join fetch ak.permissions " +
                "join fetch ak.user " +
                "where ak.key = :someKey";
            static final String GET_BY_USER_AND_LABEL =
                "select ak from AccessKey ak " +
                        "left join fetch ak.permissions " +
                        "join fetch ak.user u " +
                        "where u.id = :userId and ak.label = :label";
            static final String DELETE_BY_ID_AND_USER =
                "delete from AccessKey ak where ak.user.id = :userId and ak.id = :accessKeyId";
            static final String DELETE_BY_ID = "delete from AccessKey ak where ak.id = :accessKeyId";
            static final String DELETE_OLDER_THAN = "delete from AccessKey ak where ak.expirationDate < :expirationDate";
        }

        public static interface Parameters {

            static final String ID = "id";
            static final String USER_ID = "userId";
            static final String USER = "user";
            static final String ACCESS_KEY_ID = "accessKeyId";
            static final String KEY = "someKey";
            static final String LABEL = "label";
            static final String EXPIRATION_DATE = "expirationDate";
            static final String TYPE = "type";
        }
    }
}
