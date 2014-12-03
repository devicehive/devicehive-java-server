package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_CONFIG;

/**
 * Created by tmatvienko on 12/2/14.
 */
public class ApiConfig implements HiveEntity {

    private static final long serialVersionUID = -4819848129715601667L;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private IdentityProviderConfig google;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private IdentityProviderConfig facebook;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private IdentityProviderConfig github;

    public ApiConfig() {
    }

    public IdentityProviderConfig getGoogle() {
        return google;
    }

    public void setGoogle(IdentityProviderConfig google) {
        this.google = google;
    }

    public IdentityProviderConfig getFacebook() {
        return facebook;
    }

    public void setFacebook(IdentityProviderConfig facebook) {
        this.facebook = facebook;
    }

    public IdentityProviderConfig getGithub() {
        return github;
    }

    public void setGithub(IdentityProviderConfig github) {
        this.github = github;
    }
}
