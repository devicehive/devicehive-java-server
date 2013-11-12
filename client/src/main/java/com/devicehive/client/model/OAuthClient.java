package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class OAuthClient implements HiveEntity {

    private static final long serialVersionUID = -1095382534684298244L;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_PUBLISHED, OAUTH_GRANT_LISTED})
    private Long id;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_SUBMITTED, OAUTH_GRANT_LISTED, OAUTH_CLIENT_SUBMITTED,
            OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> name;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_SUBMITTED, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> domain;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_SUBMITTED, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> subnet;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_SUBMITTED, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> redirectUri;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_SUBMITTED, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private NullableWrapper<String> oauthId;
    @JsonPolicyDef({OAUTH_CLIENT_LISTED, OAUTH_CLIENT_PUBLISHED, OAUTH_GRANT_LISTED})
    private NullableWrapper<String> oauthSecret;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return NullableWrapper.value(name);
    }

    public void setName(String name) {
        this.name = NullableWrapper.create(name);
    }

    public String getDomain() {
        return NullableWrapper.value(domain);
    }

    public void setDomain(String domain) {
        this.domain = NullableWrapper.create(domain);
    }

    public String getSubnet() {
        return NullableWrapper.value(subnet);
    }

    public void setSubnet(String subnet) {
        this.subnet = NullableWrapper.create(subnet);
    }

    public String getRedirectUri() {
        return NullableWrapper.value(redirectUri);
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = NullableWrapper.create(redirectUri);
    }

    public String getOauthId() {
        return NullableWrapper.value(oauthId);
    }

    public void setOauthId(String oauthId) {
        this.oauthId = NullableWrapper.create(oauthId);
    }

    public String getOauthSecret() {
        return NullableWrapper.value(oauthSecret);
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = NullableWrapper.create(oauthSecret);
    }
}
