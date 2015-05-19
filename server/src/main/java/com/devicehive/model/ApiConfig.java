package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_CONFIG;

/**
 * Created by tmatvienko on 12/2/14.
 */
public class ApiConfig implements HiveEntity {

    private static final long serialVersionUID = -4819848129715601667L;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    @SerializedName("providers")
    private Set<IdentityProviderConfig> providerConfigs;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private Long sessionTimeout;

    public ApiConfig() {
    }

    public Set<IdentityProviderConfig> getProviderConfigs() {
        return providerConfigs;
    }

    public void setProviderConfigs(Set<IdentityProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
