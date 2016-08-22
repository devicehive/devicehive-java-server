package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.IDENTITY_PROVIDER_LISTED;

public class IdentityProviderVO implements HiveEntity {

    @SerializedName("name")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String name;

    @SerializedName("apiEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String apiEndpoint;

    @SerializedName("verificationEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String verificationEndpoint;

    @SerializedName("tokenEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String tokenEndpoint;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getVerificationEndpoint() {
        return verificationEndpoint;
    }

    public void setVerificationEndpoint(String verificationEndpoint) {
        this.verificationEndpoint = verificationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }
}
