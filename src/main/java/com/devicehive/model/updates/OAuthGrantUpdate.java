package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_PUBLISHED;

public class OAuthGrantUpdate implements HiveEntity {

    private static final long serialVersionUID = -2008164473287528464L;
    @SerializedName("id")
    private Long id;

    @SerializedName("client")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<OAuthClient> client;

    @SerializedName("type")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<Type> type;

    @SerializedName("accessType")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<AccessType> accessType;

    @SerializedName("redirectUri")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<String> redirectUri;

    @SerializedName("scope")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<String> scope;

    @SerializedName("networkIds")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<JsonStringWrapper> networkIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Optional<OAuthClient> getClient() {
        return client;
    }

    public void setClient(Optional<OAuthClient> client) {
        this.client = client;
    }

    public Optional<Type> getType() {
        return type;
    }

    public void setType(Optional<Type> type) {
        this.type = type;
    }

    public Optional<AccessType> getAccessType() {
        return accessType;
    }

    public void setAccessType(Optional<AccessType> accessType) {
        this.accessType = accessType;
    }

    public Optional<String> getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(Optional<String> redirectUri) {
        this.redirectUri = redirectUri;
    }

    public Optional<String> getScope() {
        return scope;
    }

    public void setScope(Optional<String> scope) {
        this.scope = scope;
    }

    public Optional<JsonStringWrapper> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Optional<JsonStringWrapper> networkIds) {
        this.networkIds = networkIds;
    }

    public OAuthGrant convertTo() {
        OAuthGrant grant = new OAuthGrant();
        grant.setId(id);
        if (client != null) {
            grant.setClient(client.orElse(null));
        }
        if (type != null) {
            grant.setType(type.orElse(null));
        }
        if (accessType != null) {
            grant.setAccessType(accessType.orElse(null));
        }
        if (redirectUri != null) {
            grant.setRedirectUri(redirectUri.orElse(null));
        }
        if (networkIds != null) {
            grant.setNetworkIds(networkIds.orElse(null));
        }
        if (scope != null) {
            grant.setScope(scope.orElse(null));
        }
        return grant;
    }


}
