package com.devicehive.vo;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.Type;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class OAuthGrantVO implements HiveEntity {
    @SerializedName("id")
    @JsonPolicyDef(
            {OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private Long id;

    @SerializedName("timestamp")
    @JsonPolicyDef(
            {OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private Date timestamp;

    @SerializedName("authCode")
    @Size(min = 35, max = 36, message = "Field cannot be empty. The length of authCode should be 36 symbols.")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_CODE})
    private String authCode;

    @SerializedName("client")
    @NotNull(message = "client field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private OAuthClientVO client;

    @SerializedName("accessKey")
    @NotNull(message = "access key field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN})
    private AccessKeyVO accessKey;

    @NotNull(message = "user field cannot be null")
    private UserVO user;

    @SerializedName("type")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private Type type;

    @SerializedName("accessType")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private com.devicehive.model.enums.AccessType accessType;

    @SerializedName("redirectUri")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect uri should not be more that " +
            "128 symbols.")
    @NotNull(message = "Redirect uri field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String redirectUri;

    @SerializedName("scope")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect uri should not be more that " +
            "128 symbols.")
    @NotNull(message = "Redirect uri field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String scope;

    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private JsonStringWrapper networkIds;

    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public OAuthClientVO getClient() {
        return client;
    }

    public void setClient(OAuthClientVO client) {
        this.client = client;
    }

    public AccessKeyVO getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(AccessKeyVO accessKey) {
        this.accessKey = accessKey;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public com.devicehive.model.enums.AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(com.devicehive.model.enums.AccessType accessType) {
        this.accessType = accessType;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public JsonStringWrapper getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds = networkIds;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Set<Long> getNetworkIdsAsSet() {
        if (networkIds == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(networkIds.getJsonString());
        if (elem instanceof JsonNull) {
            return null;
        }
        if (elem instanceof JsonArray) {
            JsonArray json = (JsonArray) elem;
            Set<Long> result = new HashSet<>(json.size());
            for (JsonElement current : json) {
                result.add(current.getAsLong());
            }
            return result;
        }
        throw new HiveException("JSON array expected!", HttpServletResponse.SC_BAD_REQUEST);
    }
}
