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
        return name.getValue();
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public String getDomain() {
        return domain.getValue();
    }

    public void setDomain(String domain) {
        this.domain.setValue(domain);
    }

    public String getSubnet() {
        return subnet.getValue();
    }

    public void setSubnet(String subnet) {
        this.subnet.setValue(subnet);
    }

    public String getRedirectUri() {
        return redirectUri.getValue();
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri.setValue(redirectUri);
    }

    public String getOauthId() {
        return oauthId.getValue();
    }

    public void setOauthId(String oauthId) {
        this.oauthId.setValue(oauthId);
    }

    public String getOauthSecret() {
        return oauthSecret.getValue();
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret.setValue(oauthSecret);
    }
}
