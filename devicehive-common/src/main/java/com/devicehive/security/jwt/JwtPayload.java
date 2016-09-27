package com.devicehive.security.jwt;

import com.devicehive.auth.HiveAction;
import com.devicehive.configuration.Constants;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Common JWT entity
 */
public class JwtPayload implements Serializable {

    private static final long serialVersionUID = -6904689203121394308L;
    public static final String JWT_CLAIM_KEY = "payload";

    //Public claims
    @SerializedName("is_admin")
    private Boolean isAdmin;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("actions")
    private Set<HiveAction> actions;

    @SerializedName("subnets")
    private Set<String> subnets;

    @SerializedName("domains")
    private Set<String> domains;

    @SerializedName("network_ids")
    private Set<Long> networkIds;

    @SerializedName("device_guids")
    private Set<String> deviceGuids;

    @SerializedName("type")
    private TokenType type;

    //Registered claims
    @SerializedName("exp")
    private Date expiration;

    private JwtPayload(Boolean isAdmin, Long userId, Set<HiveAction> actions, Set<String> subnets, Set<String> domains,
                      Set<Long> networkIds, Set<String> deviceGuids, TokenType type, Date expiration) {
        this.isAdmin = isAdmin;
        this.userId = userId;
        this.actions = actions;
        this.subnets = subnets;
        this.domains = domains;
        this.networkIds = networkIds;
        this.deviceGuids = deviceGuids;
        this.type = type;
        this.expiration = expiration;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<HiveAction> getActions() {
        return actions;
    }

    public void setActions(Set<HiveAction> actions) {
        this.actions = actions;
    }

    public Set<String> getSubnets() {
        return subnets;
    }

    public void setSubnets(Set<String> subnets) {
        this.subnets = subnets;
    }

    public Set<String> getDomains() {
        return domains;
    }

    public void setDomains(Set<String> domains) {
        this.domains = domains;
    }

    public Set<Long> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(Set<String> deviceGuids) {
        this.deviceGuids = deviceGuids;
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
        private Boolean isAdmin;
        private Long userId;
        private Set<HiveAction> actions;
        private Set<String> subnets;
        private Set<String> domains;
        private Set<Long> networkIds;
        private Set<String> deviceGuids;
        private TokenType type;
        private Date expiration;

        public Builder withPublicClaims(Long userId, Set<HiveAction> actions, Set<String> subnets, Set<String> domains,
                       Set<Long> networkIds, Set<String> deviceGuids) {
            this.userId = userId;
            this.actions = actions;
            this.subnets = subnets;
            this.domains = domains;
            this.networkIds = networkIds;
            this.deviceGuids = deviceGuids;
            return this;
        }

        public Builder withAdmin() {
            this.isAdmin = true;
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

            if (isAdmin == null) {
                isAdmin = false;
            }

            return new JwtPayload(isAdmin, userId, actions, subnets, domains, networkIds, deviceGuids, type, expiration);
        }

        public JwtPayload buildAccessToken() {
            type = TokenType.ACCESS;
            if (expiration == null) {
                expiration = new Date(System.currentTimeMillis() + Constants.DEFAULT_JWT_ACCESS_TOKEN_MAX_AGE);
            }

            if (isAdmin == null) {
                isAdmin = false;
            }

            return new JwtPayload(isAdmin, userId, actions, subnets, domains, networkIds, deviceGuids, type, expiration);
        }
    }
}
