package com.devicehive.security.jwt;

import com.devicehive.vo.AccessKeyPermissionVO;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Set;

/**
 * Common JWT entity
 */
public class JwtPrincipal implements Serializable {

    private static final long serialVersionUID = -6904689203121394308L;

    @SerializedName("role")
    private String role;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("permissions")
    private Set<AccessKeyPermissionVO> permissions;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private Long expiresIn;


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Set<AccessKeyPermissionVO> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccessKeyPermissionVO> permissions) {
        this.permissions = permissions;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
