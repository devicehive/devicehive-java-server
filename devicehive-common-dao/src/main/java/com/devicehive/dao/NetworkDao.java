package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.User;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NetworkDao {

    List<NetworkVO> findByName(String name);

    void persist(NetworkVO newNetwork);

    List<NetworkWithUsersAndDevicesVO> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> singleton, Set<Long> permittedNetworks);

    int deleteById(long id);

    NetworkVO find(@NotNull Long networkId);

    NetworkVO merge(NetworkVO existing);

    void assignToNetwork(NetworkVO network, User user);

    List<NetworkVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                       Integer skip, Optional<HivePrincipal> principal);

    Optional<NetworkVO> findFirstByName(String name);

    Optional<NetworkWithUsersAndDevicesVO> findWithUsers(@NotNull long networkId);
}
