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

public class AccessKeyBasedFilterForNetworks {

    private Set<Long> networkIds;

    public AccessKeyBasedFilterForNetworks(Collection<Long> networkIds) {
        this.networkIds = networkIds != null ? new HashSet<>(networkIds) : null;
    }

    public Set<Long> getNetworkIds() {
        return networkIds == null ? null : Collections.unmodifiableSet(networkIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessKeyBasedFilterForNetworks)) {
            return false;
        }
        AccessKeyBasedFilterForNetworks that = (AccessKeyBasedFilterForNetworks) o;
        return !(networkIds != null ? !networkIds.equals(that.networkIds) : that.networkIds != null);
    }

    @Override
    public int hashCode() {
        return networkIds != null ? networkIds.hashCode() : 0;
    }


    public static Set<AccessKeyBasedFilterForNetworks> createExtraFilters(Set<AccessKeyPermissionVO> permissionSet) {
        if (permissionSet == null) {
            return null;
        }
        Set<AccessKeyBasedFilterForNetworks> result = new HashSet<>();
        for (AccessKeyPermissionVO akp : permissionSet) {
            result.add(new AccessKeyBasedFilterForNetworks(akp.getNetworkIdsAsSet()));
        }
        return result;
    }

}
