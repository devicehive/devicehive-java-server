package com.devicehive.dao;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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

import com.devicehive.dao.riak.UserNetworkDaoRiakImpl;
import com.devicehive.dao.riak.model.UserNetwork;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private NetworkDao networkDao;

    @Autowired(required = false)
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    ApplicationContext ctx;

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(ctx.getEnvironment().acceptsProfiles("riak"));
    }


    @Test
    public void testCreate() throws Exception {
        UserVO user = new UserVO();
        user.setId(100L);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        userDao.persist(user);

        UserVO newUser = userDao.find(100L);
        assertNotNull(newUser);
    }

    @Test
    public void testDelete() throws Exception {
        UserVO user = new UserVO();
        user.setId(100L);
        userDao.persist(user);

        userDao.deleteById(100L);

        UserVO newUser = userDao.find(100L);
        assertNull(newUser);
    }

    @Test
    public void testMerge() throws Exception {
        UserVO user = new UserVO();
        user.setId(100L);
        user.setLogin("before merge");
        userDao.persist(user);

        user.setLogin("after merge");
        userDao.merge(user);

        UserVO newUser = userDao.find(100L);
        assertEquals("after merge", newUser.getLogin());
    }

    public void testFindByName() throws Exception {
        UserVO user = new UserVO();
        Long id = 100L;
        user.setId(id);
        user.setLogin("login");
        userDao.persist(user);

        Optional<UserVO> newUser = userDao.findByName("login");
        assertTrue(newUser.isPresent());
        assertEquals(id, newUser.get().getId());
    }

    @Test
    public void testHasAccessToNetwork() throws Exception {
        UserVO user = new UserVO();
        Long id = 100L;
        user.setId(id);
        userDao.persist(user);

        NetworkVO network = new NetworkVO();
        network.setId(64L);

        long hasAccess = userDao.hasAccessToNetwork(user, network);
        assertEquals(0L, hasAccess);

        UserNetwork userNetwork = new UserNetwork();
        userNetwork.setUserId(100L);
        userNetwork.setNetworkId(64L);
        userNetworkDao.persist(userNetwork);

        hasAccess = userDao.hasAccessToNetwork(user, network);
        assertEquals(1L, hasAccess);
    }

    @Test
    public void testUnassignNetwork() throws Exception {
        UserVO user = new UserVO();
        Long id = 100L;
        user.setId(id);
        userDao.persist(user);

        UserNetwork userNetwork = new UserNetwork();
        userNetwork.setUserId(100L);
        userNetwork.setNetworkId(64L);
        userNetworkDao.persist(userNetwork);

        UserNetwork userNetwork2 = new UserNetwork();
        userNetwork2.setUserId(100L);
        userNetwork2.setNetworkId(65L);
        userNetworkDao.persist(userNetwork2);

        userDao.unassignNetwork(user, 64L);

        Set<Long> userNetworks = userNetworkDao.findNetworksForUser(id);
        assertFalse(userNetworks.contains(64L));
        assertTrue(userNetworks.contains(65L));
    }
}
