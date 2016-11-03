package com.devicehive.dao.riak.model;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.AccessType;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.model.enums.Type;
import com.devicehive.vo.OAuthGrantVO;

import java.util.Date;

public class RiakOAuthGrant {

    private Long id;

    private Date timestamp;

    private String authCode;

    private OAuthClientVO client;

    private Type type;

    private AccessType accessType;

    private String redirectUri;

    private String scope;

    private JsonStringWrapper networkIds;

    private long entityVersion;

    private long userId;

    private long accessKeyId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public OAuthClientVO getClient() {
        return client;
    }

    public void setClient(OAuthClientVO client) {
        this.client = client;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public JsonStringWrapper getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds = networkIds;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public long getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(long accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    //Riak indexes
    @RiakIndex(name = "user")
    public Long getUserSi() {
        return userId;
    }

    @RiakIndex(name = "authCode")
    public String getAuthCodeSi() {
        return authCode;
    }

    public static RiakOAuthGrant convert(OAuthGrantVO grant) {
        if (grant != null) {
            RiakOAuthGrant result = new RiakOAuthGrant();
            result.setId(grant.getId());
            result.setTimestamp(grant.getTimestamp());
            result.setAuthCode(grant.getAuthCode());
            result.setClient(grant.getClient());
            result.setType(grant.getType());
            result.setAccessType(grant.getAccessType());
            result.setRedirectUri(grant.getRedirectUri());
            result.setScope(grant.getScope());
            result.setNetworkIds(grant.getNetworkIds());
            result.setEntityVersion(grant.getEntityVersion());
            result.setUserId(grant.getUser() != null ? grant.getUser().getId() : -1);
            result.setAccessKeyId(grant.getAccessKey() != null ? grant.getAccessKey().getId() : -1);
            return result;
        } else {
            return null;
        }
    }

    public static OAuthGrantVO convert(RiakOAuthGrant grant) {
        if (grant != null) {
            OAuthGrantVO result = new OAuthGrantVO();
            result.setId(grant.getId());
            result.setTimestamp(grant.getTimestamp());
            result.setAuthCode(grant.getAuthCode());
            result.setClient(grant.getClient());
            result.setType(grant.getType());
            result.setAccessType(grant.getAccessType());
            result.setRedirectUri(grant.getRedirectUri());
            result.setScope(grant.getScope());
            result.setNetworkIds(grant.getNetworkIds());
            result.setEntityVersion(grant.getEntityVersion());
            return result;
        } else {
            return null;
        }
    }
}
