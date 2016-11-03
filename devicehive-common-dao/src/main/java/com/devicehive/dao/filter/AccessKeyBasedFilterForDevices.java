package com.devicehive.dao.filter;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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

import com.devicehive.vo.AccessKeyPermissionVO;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AccessKeyBasedFilterForDevices {

    private Set<String> deviceGuids;

    private Set<Long> networkIds;

    public AccessKeyBasedFilterForDevices(Collection<String> deviceGuids, Collection<Long> networkIds) {
        this.deviceGuids = deviceGuids != null ? new HashSet<>(deviceGuids) : null;
        this.networkIds = networkIds != null ? new HashSet<>(networkIds) : null;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids == null ? null : Collections.unmodifiableSet(deviceGuids);
    }

    public Set<Long> getNetworkIds() {
        return networkIds == null ? null : Collections.unmodifiableSet(networkIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AccessKeyBasedFilterForDevices that = (AccessKeyBasedFilterForDevices) o;

        if (deviceGuids != null ? !deviceGuids.equals(that.deviceGuids) : that.deviceGuids != null) {
            return false;
        }
        if (networkIds != null ? !networkIds.equals(that.networkIds) : that.networkIds != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceGuids != null ? deviceGuids.hashCode() : 0;
        result = 31 * result + (networkIds != null ? networkIds.hashCode() : 0);
        return result;
    }

    public static Set<AccessKeyBasedFilterForDevices> createExtraFilters(Set<AccessKeyPermissionVO> permissionSet) {
        if (permissionSet == null) {
            return null;
        }
        Set<AccessKeyBasedFilterForDevices> result = new HashSet<>();
        for (AccessKeyPermissionVO akp : permissionSet) {
            result.add(new AccessKeyBasedFilterForDevices(akp.getDeviceGuidsAsSet(), akp.getNetworkIdsAsSet()));
        }
        return result;
    }
}
