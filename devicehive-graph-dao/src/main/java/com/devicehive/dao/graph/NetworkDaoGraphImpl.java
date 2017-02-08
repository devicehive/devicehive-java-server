package com.devicehive.dao.graph;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
import com.devicehive.dao.NetworkDao;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class NetworkDaoGraphImpl implements NetworkDao {
    @Override
    public List<NetworkVO> findByName(String name) {
        return null;
    }

    @Override
    public void persist(NetworkVO newNetwork) {

    }

    @Override
    public List<NetworkWithUsersAndDevicesVO> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> singleton, Set<Long> permittedNetworks) {
        return null;
    }

    @Override
    public int deleteById(long id) {
        return 0;
    }

    @Override
    public NetworkVO find(@NotNull Long networkId) {
        return null;
    }

    @Override
    public NetworkVO merge(NetworkVO existing) {
        return null;
    }

    @Override
    public void assignToNetwork(NetworkVO network, UserVO user) {

    }

    @Override
    public List<NetworkVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        return null;
    }

    @Override
    public Optional<NetworkVO> findFirstByName(String name) {
        return null;
    }

    @Override
    public Optional<NetworkWithUsersAndDevicesVO> findWithUsers(@NotNull long networkId) {
        return null;
    }
}
