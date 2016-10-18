package com.devicehive.security.jwt;

import com.devicehive.configuration.Constants;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Set;

/**
 * Common JWT entity
 * Structure must be as provided below:
 * {
 *     "userId": user_id,
 *     "actions": ["action1","action2","actionN"],
 *     "networkIds": ["id1","id2","idN"],
 *     "deviceGuids": ["guid1","guid2","guidN"],
 *     "expiration": "2016-10-13T14:56:24.067Z"
 * }
 *
 * To get admin permissions (to all actions, networks, etc) you have to specify "*" for string parameters:
 * {
 *     "userId": user_id,
 *     "actions": ["*"],
 *     "networkIds": ["*"],
 *     "deviceGuids": ["*"],
 *     "expiration": "2099-01-01T11:00:00.000Z"
 * }
 */
public class JwtPayload implements HiveEntity {

    private static final long serialVersionUID = -6904689203121394308L;
    public static final String JWT_CLAIM_KEY = "payload";

    public final static String USER_ID = "userId";
    public final static String ACTIONS = "actions";
    public final static String NETWORK_IDS = "networkIds";
    public final static String DEVICE_GUIDS = "deviceGuids";
    public final static String EXPIRATION = "expiration";

    //Public claims

    @SerializedName("userId")
    private Long userId;

    @SerializedName("actions")
    private Set<String> actions;

    @SerializedName("networkIds")
    private Set<String> networkIds;

    @SerializedName("deviceGuids")
    private Set<String> deviceGuids;

    //Registered claims
    @SerializedName("expiration")
    private Date expiration;

    private JwtPayload(Long userId, Set<String> actions, Set<String> networkIds,
                       Set<String> deviceGuids, Date expiration) {
        this.userId = userId;
        this.actions = actions;
        this.networkIds = networkIds;
        this.deviceGuids = deviceGuids;
        this.expiration = expiration;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<String> getActions() {
        return actions;
    }

    public void setActions(Set<String> actions) {
        this.actions = actions;
    }

    public Set<String> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<String> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(Set<String> deviceGuids) {
        this.deviceGuids = deviceGuids;
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
        private Long userId;
        private Set<String> actions;
        private Set<String> networkIds;
        private Set<String> deviceGuids;
        private Date expiration;

        public Builder withPublicClaims(Long userId, Set<String> actions,
                                        Set<String> networkIds, Set<String> deviceGuids) {
            this.userId = userId;
            this.actions = actions;
            this.networkIds = networkIds;
            this.deviceGuids = deviceGuids;
            return this;
        }

        public Builder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder withActions(Set<String> actions) {
            this.actions = actions;
            return this;
        }

        public Builder withNetworkIds(Set<String> networkIds) {
            this.networkIds = networkIds;
            return this;
        }

        public Builder withDeviceGuids(Set<String> deviceGuids) {
            this.deviceGuids = deviceGuids;
            return this;
        }

        public Builder withExpirationDate(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public JwtPayload buildRefreshToken() {
            if (expiration == null) {
                expiration = new Date(System.currentTimeMillis() + Constants.DEFAULT_JWT_REFRESH_TOKEN_MAX_AGE);
            }

            return new JwtPayload(userId, actions, networkIds, deviceGuids, expiration);
        }

        public JwtPayload buildPayload() {
            if (expiration == null) {
                expiration = new Date(System.currentTimeMillis() + Constants.DEFAULT_JWT_ACCESS_TOKEN_MAX_AGE);
            }

            return new JwtPayload(userId, actions, networkIds, deviceGuids, expiration);
        }
    }
}
