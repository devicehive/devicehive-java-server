package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.Network;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface NetworkDao {

    List<Network> findByName(String name);

    void persist(Network newNetwork);

    List<Network> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> singleton, Set<Long> permittedNetworks);

    int deleteById(long id);

    Network find(@NotNull Long networkId);

    Network merge(Network existing);

    List<Network> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                       Integer skip, Optional<HivePrincipal> principal);

    Optional<Network> findFirstByName(String name);

    Optional<Network> findWithUsers(@NotNull long networkId);
}
