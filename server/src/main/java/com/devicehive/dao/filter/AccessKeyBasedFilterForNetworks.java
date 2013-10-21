package com.devicehive.dao.filter;


import com.devicehive.model.AccessKeyPermission;

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
        if (this == o) return true;
        if (!(o instanceof AccessKeyBasedFilterForNetworks)) return false;
        AccessKeyBasedFilterForNetworks that = (AccessKeyBasedFilterForNetworks) o;
        return !(networkIds != null ? !networkIds.equals(that.networkIds) : that.networkIds != null);
    }

    @Override
    public int hashCode() {
        return networkIds != null ? networkIds.hashCode() : 0;
    }


    public static Set<AccessKeyBasedFilterForNetworks> createExtraFilters(Set<AccessKeyPermission> permissionSet) {
        if (permissionSet == null) {
            return null;
        }
        Set<AccessKeyBasedFilterForNetworks> result = new HashSet<>();
        for (AccessKeyPermission akp : permissionSet) {
            result.add(new AccessKeyBasedFilterForNetworks(akp.getNetworkIdsAsSet()));
        }
        return result;
    }

}
