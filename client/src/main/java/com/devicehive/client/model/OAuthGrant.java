package com.devicehive.client.model;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents an OAuth permission grant.
 * See <a href="http://www.devicehive.com/restful#Reference/OAuthGrant">OAuthGrant</a> for more details
 */
public class OAuthGrant implements HiveEntity {

    private static final long serialVersionUID = 6725932065321755993L;
    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private Long id;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private NullableWrapper<Timestamp> timestamp;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_CODE})
    private NullableWrapper<String> authCode;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<OAuthClient> client;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN})
    private NullableWrapper<AccessKey> accessKey;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<OAuthType> type;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<AccessType> accessType;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> redirectUri;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> scope;

    @JsonPolicyDef({OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<JsonStringWrapper> networkIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp.getValue());
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp.setValue(ObjectUtils.cloneIfPossible(timestamp));
    }

    public String getAuthCode() {
        return NullableWrapper.value(authCode);
    }

    public void setAuthCode(String authCode) {
        this.authCode = NullableWrapper.create(authCode);
    }

    public OAuthClient getClient() {
        return NullableWrapper.value(client);
    }

    public void setClient(OAuthClient client) {
        this.client = NullableWrapper.create(client);
    }

    public AccessKey getAccessKey() {
        return NullableWrapper.value(accessKey);
    }

    public void setAccessKey(AccessKey accessKey) {
        this.accessKey = NullableWrapper.create(accessKey);
    }

    public OAuthType getType() {
        return NullableWrapper.value(type);
    }

    public void setType(OAuthType oauthType) {
        this.type = NullableWrapper.create(oauthType);
    }

    public AccessType getAccessType() {
        return NullableWrapper.value(accessType);
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = NullableWrapper.create(accessType);
    }

    public String getRedirectUri() {
        return NullableWrapper.value(redirectUri);
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = NullableWrapper.create(redirectUri);
    }

    public String getScope() {
        return NullableWrapper.value(scope);
    }

    public void setScope(String scope) {
        this.scope = NullableWrapper.create(scope);
    }

    public JsonStringWrapper getNetworkIds() {
        return NullableWrapper.value(networkIds);
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds = NullableWrapper.create(networkIds);
    }

    public Set<Long> getNetworkIdsAsSet() {
        if (networkIds == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(networkIds.getValue().getJsonString());
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
        throw new IllegalArgumentException("JSON array expected!");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OAuthGrant{");
        sb.append("id=").append(id);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", client=").append(client);
        sb.append(", accessKey=").append(accessKey);
        sb.append(", type=").append(type);
        sb.append(", accessType=").append(accessType);
        sb.append(", redirectUri=").append(redirectUri);
        sb.append(", scope=").append(scope);
        sb.append(", networkIds=").append(networkIds);
        sb.append('}');
        return sb.toString();
    }
}
