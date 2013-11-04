package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.ObjectUtils;

import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class OAuthGrant implements HiveEntity {

    private static final long serialVersionUID = 6725932065321755993L;
    @JsonPolicyDef(
            {OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private Long id;
    @JsonPolicyDef(
            {OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
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
        return authCode.getValue();
    }

    public void setAuthCode(String authCode) {
        this.authCode.setValue(authCode);
    }

    public OAuthClient getClient() {
        return client.getValue();
    }

    public void setClient(OAuthClient client) {
        this.client.setValue(client);
    }

    public AccessKey getAccessKey() {
        return accessKey.getValue();
    }

    public void setAccessKey(AccessKey accessKey) {
        this.accessKey.setValue(accessKey);
    }

    public OAuthType getType() {
        return type.getValue();
    }

    public void setType(OAuthType OAuthType) {
        this.type.setValue(OAuthType);
    }

    public AccessType getAccessType() {
        return accessType.getValue();
    }

    public void setAccessType(AccessType accessType) {
        this.accessType.setValue(accessType);
    }

    public String getRedirectUri() {
        return redirectUri.getValue();
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri.setValue(redirectUri);
    }

    public String getScope() {
        return scope.getValue();
    }

    public void setScope(String scope) {
        this.scope.setValue(scope);
    }

    public JsonStringWrapper getNetworkIds() {
        return networkIds.getValue();
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds.setValue(networkIds);
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
        throw new HiveClientException("JSON array expected!", Response.Status.BAD_REQUEST.getStatusCode());
    }
}
