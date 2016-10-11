package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.JWT_TOKEN_SUBMITTED;

/**
 * JWT token response entity
 */
public class JwtTokenVO implements HiveEntity {

    private static final long serialVersionUID = 1934838110938833117L;

    @JsonPolicyDef({JWT_TOKEN_SUBMITTED})
    @SerializedName("jwt_token")
    private String token;

    public JwtTokenVO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
