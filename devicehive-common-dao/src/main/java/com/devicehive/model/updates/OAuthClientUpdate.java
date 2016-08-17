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
import com.devicehive.vo.OAuthClientVO;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_CLIENT_PUBLISHED;

public class OAuthClientUpdate implements HiveEntity {

    private static final long serialVersionUID = -5522057352642320219L;
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private Optional<String> name;

    @SerializedName("domain")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private Optional<String> domain;

    @SerializedName("subnet")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private Optional<String> subnet;

    @SerializedName("redirectUri")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private Optional<String> redirectUri;

    @SerializedName("oauthId")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private Optional<String> oauthId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<String> getDomain() {
        return domain;
    }

    public void setDomain(Optional<String> domain) {
        this.domain = domain;
    }

    public Optional<String> getSubnet() {
        return subnet;
    }

    public void setSubnet(Optional<String> subnet) {
        this.subnet = subnet;
    }

    public Optional<String> getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(Optional<String> redirectUri) {
        this.redirectUri = redirectUri;
    }

    public Optional<String> getOauthId() {
        return oauthId;
    }

    public void setOauthId(Optional<String> oauthId) {
        this.oauthId = oauthId;
    }

    public OAuthClientVO convertTo() {
        OAuthClientVO client = new OAuthClientVO();
        client.setId(id);
        if (name != null) {
            client.setName(name.orElse(null));
        }
        if (domain != null) {
            client.setDomain(domain.orElse(null));
        }
        if (subnet != null) {
            client.setSubnet(subnet.orElse(null));
        }
        if (redirectUri != null) {
            client.setRedirectUri(redirectUri.orElse(null));
        }
        if (oauthId != null) {
            client.setOauthId(oauthId.orElse(null));
        }
        return client;
    }
}
