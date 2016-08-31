package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.io.Serializable;
import java.util.Objects;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_CONFIG;

/**
 * Created by tmatvienko on 12/2/14.
 */
public class IdentityProviderConfig implements Serializable {

    private static final long serialVersionUID = -2274848199115698341L;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private String name;

    @JsonPolicyDef(REST_SERVER_CONFIG)
    private String clientId;

    public IdentityProviderConfig(String name) {
        this.name = name;
    }

    public IdentityProviderConfig(String name, String clientId) {
        this.name = name;
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityProviderConfig)) return false;
        IdentityProviderConfig that = (IdentityProviderConfig) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clientId);
    }

    @Override
    public String toString() {
        return "IdentityProviderConfig{" +
                "name='" + name + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}
