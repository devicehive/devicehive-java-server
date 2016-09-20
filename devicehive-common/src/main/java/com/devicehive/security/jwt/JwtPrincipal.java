package com.devicehive.security.jwt;

import com.devicehive.vo.AccessKeyPermissionVO;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Common JWT entity
 */
public class JwtPrincipal implements Serializable {

    private static final long serialVersionUID = -6904689203121394308L;

    //Public claims

    @SerializedName("role")
    private String role;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("permissions")
    private Set<AccessKeyPermissionVO> permissions;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    //Registered claims

    // This will define the expiration in NumericDate value. The expiration MUST be after the current date/time.
    @SerializedName("exp")
    private Date expiration;

    //The time the JWT was issued. Can be used to determine the age of the JWT
    @SerializedName("iat")
    private Date issuedAt;

    public JwtPrincipal(String role, String clientId, String refreshToken, Date expiration) {
        this.role = role;
        this.clientId = clientId;
        this.refreshToken = refreshToken;
        this.expiration = expiration;
    }

    public JwtPrincipal(String clientId, String role, String accessToken, String tokenType, Date expiration, Date issuedAt) {
        this.role = role;
        this.clientId = clientId;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiration = expiration;
        this.issuedAt = issuedAt;
    }

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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }
}
