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
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;

import java.util.Date;
import java.util.Set;

public class RiakAccessKey {

    private Long id;

    private String label;

    private String key;

    private RiakUser user;

    private Date expirationDate;

    private AccessKeyType type;

    private Set<RiakAccessKeyPermission> permissions;

    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public RiakUser getUser() {
        return user;
    }

    public void setUser(RiakUser user) {
        this.user = user;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public AccessKeyType getType() {
        return type;
    }

    public void setType(AccessKeyType type) {
        this.type = type;
    }

    public Set<RiakAccessKeyPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<RiakAccessKeyPermission> permissions) {
        this.permissions = permissions;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @RiakIndex(name = "label")
    public String getLabelSi() {
        return label;
    }

    @RiakIndex(name = "userId")
    public long getUserIdSi() {
        return user.getId();
    }

    @RiakIndex(name = "key")
    public String getKeySi() {
        return key;
    }

    @RiakIndex(name = "expirationDate")
    public Long getExpirationDateSi() {
        return expirationDate != null ? expirationDate.getTime() : -1;
    }

    public static RiakAccessKey convert(AccessKeyVO accessKey) {
        RiakAccessKey result = null;
        if (accessKey != null) {
            result = new RiakAccessKey();
            result.setId(accessKey.getId());
            result.setLabel(accessKey.getLabel());
            result.setKey(accessKey.getKey());
            RiakUser user = RiakUser.convertToEntity(accessKey.getUser());
            result.setUser(user);
            result.setExpirationDate(accessKey.getExpirationDate());
            result.setType(accessKey.getType());
            Set<RiakAccessKeyPermission> permissions = RiakAccessKeyPermission.convertToEntity(accessKey.getPermissions());
            result.setPermissions(permissions);
            result.setEntityVersion(accessKey.getEntityVersion());
        }
        return result;
    }

    public static AccessKeyVO convert(RiakAccessKey accessKey) {
        AccessKeyVO result = null;
        if (accessKey != null) {
            result = new AccessKeyVO();
            result.setId(accessKey.getId());
            result.setLabel(accessKey.getLabel());
            result.setKey(accessKey.getKey());
            UserVO user = RiakUser.convertToVo(accessKey.getUser());
            result.setUser(user);
            result.setExpirationDate(accessKey.getExpirationDate());
            result.setType(accessKey.getType());
            Set<AccessKeyPermissionVO> permissions = RiakAccessKeyPermission.converttoVO(accessKey.getPermissions());
            result.setPermissions(permissions);
            result.setEntityVersion(accessKey.getEntityVersion());
        }
        return result;
    }
}
