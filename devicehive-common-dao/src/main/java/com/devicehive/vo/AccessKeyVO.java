package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.User;
import com.devicehive.model.enums.AccessKeyType;
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_LISTED;

public class AccessKeyVO implements HiveEntity {

    @JsonPolicyDef({ACCESS_KEY_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_LISTED_ADMIN,
            OAUTH_GRANT_LISTED, ACCESS_KEY_SUBMITTED})
    private Long id;

    @NotNull(message = "Label column cannot be null")
    @Size(min = 1, max = 64,
            message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private String label;

    @NotNull(message = "Key column cannot be null")
    @Size(min = 1, max = 48,
            message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_SUBMITTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_LISTED_ADMIN,
            OAUTH_GRANT_LISTED})
    private String key;

    @NotNull(message = "User column cannot be null")
    private User user;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private Date expirationDate;

    @SerializedName("type")
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private AccessKeyType type;

    @NotNull
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private Set<AccessKeyPermission> permissions;

    private long entityVersion;

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
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public AccessKeyType getType() {
        return type;
    }

    public void setType(AccessKeyType type) {
        this.type = type;
    }

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
}
