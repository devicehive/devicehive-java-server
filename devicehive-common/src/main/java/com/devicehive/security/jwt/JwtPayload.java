package com.devicehive.security.jwt;

import com.devicehive.configuration.Constants;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Common JWT entity
 */
public class JwtPayload implements Serializable {

    private static final long serialVersionUID = -6904689203121394308L;

    //Public claims

    @SerializedName("role")
    private String role;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("permissions")
    private Set<AccessKeyPermissionVO> permissions;

    @SerializedName("token")
    private String token;

    @SerializedName("type")
    private TokenType type;

    //Registered claims

    // This will define the expiration in NumericDate value. The expiration MUST be after the current date/time.
    @SerializedName("exp")
    private Date expiration;

    private JwtPayload(String role, String clientId, Set<AccessKeyPermissionVO> permissions, String token, TokenType type, Date expiration) {
        this.role = role;
        this.clientId = clientId;
        this.permissions = permissions;
        this.token = token;
        this.type = type;
        this.expiration = expiration;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Set<AccessKeyPermissionVO> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccessKeyPermissionVO> permissions) {
        this.permissions = permissions;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String role;
        private String clientId;
        private Set<AccessKeyPermissionVO> permissions;
        private String token;
        private TokenType type;
        private Date expiration;

        public Builder withPublicClaims(String role, String clientId, Set<AccessKeyPermissionVO> permissions, String token) {
            this.role = role;
            this.clientId = clientId;
            this.permissions = permissions;
            this.token = token;
            return this;
        }

        public Builder withExpirationDate(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public JwtPayload buildRefreshToken() {
            type = TokenType.REFRESH;
            if (expiration == null) {
                expiration = new Date(System.currentTimeMillis() + Constants.DEFAULT_JWT_REFRESH_TOKEN_MAX_AGE);
            }

            return new JwtPayload(role, clientId, permissions, token, type, expiration);
        }

        public JwtPayload buildAccessToken() {
            type = TokenType.ACCESS;
            if (expiration == null) {
                expiration = new Date(System.currentTimeMillis() + Constants.DEFAULT_JWT_ACCESS_TOKEN_MAX_AGE);
            }

            return new JwtPayload(role, clientId, permissions, token, type, expiration);
        }
    }
}
