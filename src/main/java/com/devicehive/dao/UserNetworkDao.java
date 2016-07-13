package com.devicehive.dao;
import com.devicehive.model.UserNetwork;

import java.util.Set;

public interface UserNetworkDao {

    void persist(UserNetwork userNetwork);

    UserNetwork merge(UserNetwork existing);

    Set<Long> findNetworksForUser(Long userId);

    Set<Long> findUsersInNetwork(Long networkId);
}
