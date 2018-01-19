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

import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithDeviceTypeVO;
import com.devicehive.vo.UserWithNetworkVO;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<UserVO> findByName(String name);

    long hasAccessToNetwork(UserVO user, NetworkVO network);

    long hasAccessToDevice(UserVO user, String deviceId);

    UserWithNetworkVO getWithNetworksById(long id);

    UserWithDeviceTypeVO getWithDeviceTypeById(long id);

    int deleteById(long id);

    UserVO find(Long id);

    void persist(UserVO user);

    UserVO merge(UserVO existing);

    void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId);

    void unassignDeviceType(@NotNull UserVO existingUser, @NotNull long deviceId);

    UserVO allowAllDeviceTypes(@NotNull UserWithDeviceTypeVO existingUser);

    UserVO disallowAllDeviceTypes(@NotNull UserVO existingUser);

    List<UserVO> list(String login, String loginPattern, Integer role, Integer status, String sortField,
                       boolean sortOrderAsc, Integer take, Integer skip);

    long count(String login, String loginPattern, Integer role, Integer status);
}
