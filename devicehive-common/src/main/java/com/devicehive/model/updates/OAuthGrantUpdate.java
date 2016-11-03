package com.devicehive.model.updates;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.vo.OAuthGrantVO;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_PUBLISHED;

public class OAuthGrantUpdate implements HiveEntity {

    private static final long serialVersionUID = -2008164473287528464L;
    @SerializedName("id")
    private Long id;

    @SerializedName("client")
    @JsonPolicyDef({OAUTH_GRANT_PUBLISHED})
    private Optional<OAuthClientVO> client;

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

    public Optional<OAuthClientVO> getClient() {
        return client;
    }

    public void setClient(Optional<OAuthClientVO> client) {
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

    public OAuthGrantVO convertTo() {
        OAuthGrantVO grant = new OAuthGrantVO();
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
