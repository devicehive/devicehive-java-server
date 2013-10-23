package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_PUBLISHED;

public class OAuthGrantUpdate implements HiveEntity {

    private static final long serialVersionUID = -2008164473287528464L;
    @SerializedName("id")
    private Long id;

    @SerializedName("client")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<OAuthClient> client;

    @SerializedName("type")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<Type> type;

    @SerializedName("accessType")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<AccessType> accessType;

    @SerializedName("redirectUri")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> redirectUri;

    @SerializedName("scope")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> scope;

    @SerializedName("networkIds")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<JsonStringWrapper> networkIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NullableWrapper<OAuthClient> getClient() {
        return client;
    }

    public void setClient(NullableWrapper<OAuthClient> client) {
        this.client = client;
    }

    public NullableWrapper<Type> getType() {
        return type;
    }

    public void setType(NullableWrapper<Type> type) {
        this.type = type;
    }

    public NullableWrapper<AccessType> getAccessType() {
        return accessType;
    }

    public void setAccessType(NullableWrapper<AccessType> accessType) {
        this.accessType = accessType;
    }

    public NullableWrapper<String> getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(NullableWrapper<String> redirectUri) {
        this.redirectUri = redirectUri;
    }

    public NullableWrapper<String> getScope() {
        return scope;
    }

    public void setScope(NullableWrapper<String> scope) {
        this.scope = scope;
    }

    public NullableWrapper<JsonStringWrapper> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(NullableWrapper<JsonStringWrapper> networkIds) {
        this.networkIds = networkIds;
    }

    public OAuthGrant convertTo() {
        OAuthGrant grant = new OAuthGrant();
        grant.setId(id);
        if (client != null) {
            grant.setClient(client.getValue());
        }
        if (type != null) {
            grant.setType(type.getValue());
        }
        if (accessType != null) {
            grant.setAccessType(accessType.getValue());
        }
        if (redirectUri != null) {
            grant.setRedirectUri(redirectUri.getValue());
        }
        if (networkIds != null) {
            grant.setNetworkIds(networkIds.getValue());
        }
        if (scope != null){
            grant.setScope(scope.getValue());
        }
        return grant;
    }


}
