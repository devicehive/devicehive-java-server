package com.devicehive.vo;

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
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class OAuthClientVO implements HiveEntity {
    @SerializedName("id")
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_CLIENT_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN,
            OAUTH_GRANT_LISTED})
    private Long id;


    @SerializedName("name")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
            OAUTH_GRANT_PUBLISHED})
    private String name;

    @SerializedName("domain")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of domain should not be more than 128 " +
            "symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
            OAUTH_GRANT_PUBLISHED})
    private String domain;

    @SerializedName("subnet")
    @Size(max = 128, message = "Field cannot be empty. The length of subnet should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
            OAUTH_GRANT_PUBLISHED})
    private String subnet;

    @SerializedName("redirectUri")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect URI should not be more than " +
            "128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
            OAUTH_GRANT_PUBLISHED})
    private String redirectUri;

    @SerializedName("oauthId")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of oauth id should not be more " +
            "than 128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
            OAUTH_GRANT_PUBLISHED})
    private String oauthId;

    @SerializedName("oauthSecret")
    @Size(min = 24, max = 32, message = "Field cannot be empty. The length of oauth secret should not be " +
            "more than 128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN})
    private String oauthSecret;

    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getOauthId() {
        return oauthId;
    }

    public void setOauthId(String oauthId) {
        this.oauthId = oauthId;
    }

    public String getOauthSecret() {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }

}
