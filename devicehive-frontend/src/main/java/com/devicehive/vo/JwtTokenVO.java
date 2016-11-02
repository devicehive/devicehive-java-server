package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.JWT_ACCESS_TOKEN_SUBMITTED;

/**
 * JWT token response entity
 */
public class JwtTokenVO implements HiveEntity {

    private static final long serialVersionUID = 1934838110938833117L;

    @JsonPolicyDef({JWT_REFRESH_TOKEN_SUBMITTED, JWT_ACCESS_TOKEN_SUBMITTED})
    @ApiModelProperty(hidden = true)
    @SerializedName("access_token")
    private String accessToken; 

    @JsonPolicyDef({JWT_REFRESH_TOKEN_SUBMITTED})
    @SerializedName("refresh_token")
    private String refreshToken;

    public JwtTokenVO() {
    }

    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
