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

import com.devicehive.dao.UserDao;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoGraphImpl implements UserDao {

    @Override
    public Optional<UserVO> findByName(String name) {
        return null;
    }

    @Override
    public UserVO findByGoogleName(String name) {
        return null;
    }

    @Override
    public UserVO findByFacebookName(String name) {
        return null;
    }

    @Override
    public UserVO findByGithubName(String name) {
        return null;
    }

    @Override
    public Optional<UserVO> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin) {
        return null;
    }

    @Override
    public long hasAccessToNetwork(UserVO user, NetworkVO network) {
        return 0;
    }

    @Override
    public long hasAccessToDevice(UserVO user, String deviceGuid) {
        return 0;
    }

    @Override
    public UserWithNetworkVO getWithNetworksById(long id) {
        return null;
    }

    @Override
    public int deleteById(long id) {
        return 0;
    }

    @Override
    public UserVO find(Long id) {
        return null;
    }

    @Override
    public void persist(UserVO user) {

    }

    @Override
    public UserVO merge(UserVO existing) {
        return null;
    }

    @Override
    public void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId) {

    }

    @Override
    public List<UserVO> list(String login, String loginPattern, Integer status, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        return null;
    }
}
