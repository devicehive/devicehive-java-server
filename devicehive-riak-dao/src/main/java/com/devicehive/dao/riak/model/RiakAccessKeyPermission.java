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

import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.AccessKeyPermissionVO;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class RiakAccessKeyPermission {

    private Long id;

    private JsonStringWrapper domains;

    private JsonStringWrapper subnets;

    private JsonStringWrapper actions;

    private JsonStringWrapper networkIds;

    private JsonStringWrapper deviceGuids;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JsonStringWrapper getDomains() {
        return domains;
    }

    public void setDomains(JsonStringWrapper domains) {
        this.domains = domains;
    }

    public JsonStringWrapper getSubnets() {
        return subnets;
    }

    public void setSubnets(JsonStringWrapper subnets) {
        this.subnets = subnets;
    }

    public JsonStringWrapper getActions() {
        return actions;
    }

    public void setActions(JsonStringWrapper actions) {
        this.actions = actions;
    }

    public JsonStringWrapper getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds = networkIds;
    }

    public JsonStringWrapper getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(JsonStringWrapper deviceGuids) {
        this.deviceGuids = deviceGuids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RiakAccessKeyPermission that = (RiakAccessKeyPermission) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public static RiakAccessKeyPermission convert(AccessKeyPermissionVO accessKey) {
        RiakAccessKeyPermission result = null;
        if (accessKey != null) {
            result = new RiakAccessKeyPermission();
            result.setId(accessKey.getId());
            result.setActions(accessKey.getActions());
            result.setDeviceGuids(accessKey.getDeviceGuids());
            result.setDomains(accessKey.getDomains());
            result.setNetworkIds(accessKey.getNetworkIds());
            result.setSubnets(accessKey.getSubnets());
        }
        return result;
    }

    public static AccessKeyPermissionVO convert(RiakAccessKeyPermission accessKey) {
        AccessKeyPermissionVO result = null;
        if (accessKey != null) {
            result = new AccessKeyPermissionVO();
            result.setId(accessKey.getId());
            result.setActions(accessKey.getActions());
            result.setDeviceGuids(accessKey.getDeviceGuids());
            result.setDomains(accessKey.getDomains());
            result.setNetworkIds(accessKey.getNetworkIds());
            result.setSubnets(accessKey.getSubnets());
        }
        return result;
    }

    public static Set<RiakAccessKeyPermission> convertToEntity(Collection<AccessKeyPermissionVO> accessKeys) {
        Set<RiakAccessKeyPermission> result = null;
        if (accessKeys != null) {
            result = accessKeys.stream().map(RiakAccessKeyPermission::convert).collect(Collectors.toSet());
        } else {
            result = Collections.emptySet();
        }
        return result;
    }

    public static Set<AccessKeyPermissionVO> converttoVO(Collection<RiakAccessKeyPermission> accessKeys) {
        Set<AccessKeyPermissionVO> result = null;
        if (accessKeys != null) {
            result = accessKeys.stream().map(RiakAccessKeyPermission::convert).collect(Collectors.toSet());
        } else {
            result = Collections.emptySet();
        }
        return result;
    }
}
