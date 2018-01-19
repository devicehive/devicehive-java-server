package com.devicehive.dao;

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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;

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

    void assignToNetwork(NetworkVO network, UserVO user);

    List<NetworkVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                       Integer skip, Optional<HivePrincipal> principal);

    long count(String name, String namePattern, HivePrincipal principal);

    Optional<NetworkVO> findFirstByName(String name);

    Optional<NetworkWithUsersAndDevicesVO> findWithUsers(@NotNull long networkId);

    Optional<NetworkVO> findDefaultByUser(long userId);

}
