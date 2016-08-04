package com.devicehive.dao;

import com.devicehive.dao.riak.UserNetworkDaoRiakImpl;
import com.devicehive.model.User;
import com.devicehive.dao.riak.model.UserNetwork;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class NetworkDaoTest extends AbstractResourceTest {

    @Autowired
    private NetworkDao networkDao;

    @Autowired
    private UserDao userDao;

    @Autowired(required = false)
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    private ApplicationContext context;

    @Before
    public void beforeMethod() {
        Assume.assumeTrue(context.getEnvironment().acceptsProfiles("riak"));
    }

    @Test
    public void shouldCreateNetwork() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setKey(RandomStringUtils.randomAlphabetic(10));
        network.setName(RandomStringUtils.randomAlphabetic(10));

        networkDao.persist(network);

        NetworkVO created = networkDao.find(network.getId());
        assertThat(created, notNullValue());
        assertThat(created.getKey(), equalTo(network.getKey()));
        assertThat(created.getName(), equalTo(network.getName()));
    }

    @Test
    public void shouldUpdateNetwork() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setKey(RandomStringUtils.randomAlphabetic(10));

        networkDao.persist(network);

        NetworkVO created = networkDao.find(network.getId());
        assertThat(created, notNullValue());
        assertThat(created.getKey(), equalTo(network.getKey()));

        created.setKey(RandomStringUtils.randomAlphabetic(20));
        networkDao.merge(created);

        NetworkVO updated = networkDao.find(created.getId());
        assertThat(updated, notNullValue());
        assertThat(updated.getId(), equalTo(created.getId()));
        assertThat(updated.getKey(), equalTo(created.getKey()));
        assertThat(updated.getKey(), not(equalTo(network.getKey())));
    }

    @Test
    public void shouldDeleteNetwork() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setKey(RandomStringUtils.randomAlphabetic(10));

        networkDao.persist(network);

        assertNotNull(networkDao.find(network.getId()));

        networkDao.deleteById(network.getId());

        assertNull(networkDao.find(network.getId()));
    }

    @Test
    public void shouldFindByName() throws Exception {
        String name = RandomStringUtils.randomAlphabetic(10);
        int count = 0;
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setKey(RandomStringUtils.randomAlphabetic(20));
            network.setDescription(RandomStringUtils.randomAlphabetic(20));
            if (i % 2 == 0) {
                network.setName(name);
                count++;
            } else {
                network.setName(RandomStringUtils.randomAlphabetic(10));
            }
            networkDao.persist(network);
        }
        List<NetworkVO> networks = networkDao.findByName(name);
        assertThat(networks, hasSize(count));
        Set<String> names = networks.stream().map(NetworkVO::getName).collect(Collectors.toSet());
        assertThat(names, hasSize(1));
        assertThat(names, hasItem(name));
    }

    @Test
    public void shouldFindById() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setKey(RandomStringUtils.randomAlphabetic(10));
        networkDao.persist(network);
        assertThat(network.getId(), notNullValue());

        NetworkVO found = networkDao.find(network.getId());
        assertThat(found, notNullValue());
        assertThat(network.getId(), equalTo(found.getId()));
        assertThat(network.getKey(), equalTo(found.getKey()));
    }

    @Test
    public void shouldGetNetworksByIdsSet() throws Exception {
        Set<Long> networkIds = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setKey(RandomStringUtils.randomAlphabetic(10));
            networkDao.persist(network);
            if (i % 2 == 0) {
                networkIds.add(network.getId());
            }
        }

        List<NetworkWithUsersAndDevicesVO> networks = networkDao.getNetworksByIdsAndUsers(null, networkIds, null);
        assertNotNull(networks);
        assertThat(networks, hasSize(networkIds.size()));
        Set<Long> returnedIds = networks.stream().map(NetworkWithUsersAndDevicesVO::getId).collect(Collectors.toSet());
        assertThat(networkIds, equalTo(returnedIds));
    }

    @Test
    public void shouldGetNetworksWithUserFilter() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        userDao.persist(user);
        assertNotNull(user.getId());

        Set<Long> networkIds = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setKey(RandomStringUtils.randomAlphabetic(10));
            networkDao.persist(network);
            if (i % 2 == 0) {
                UserNetwork un = new UserNetwork();
                un.setNetworkId(network.getId());
                un.setUserId(user.getId());
                userNetworkDao.persist(un);
            }
            networkIds.add(network.getId());
        }

        List<NetworkWithUsersAndDevicesVO> networks = networkDao.getNetworksByIdsAndUsers(user.getId(), networkIds, null);
        assertNotNull(networks);
        assertThat(networks, hasSize(50));
        networks.forEach(n -> {
            assertNotNull(n.getUsers());
            assertThat(n.getUsers(), hasSize(1));
            assertThat(n.getUsers().stream().findFirst().get().getId(), equalTo(user.getId()));
        });
    }

    @Test
    public void shouldGetOnlyPermittedNetworks() throws Exception {
        Set<Long> networkIds = new HashSet<>();
        Set<Long> permitted = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setKey(RandomStringUtils.randomAlphabetic(10));
            networkDao.persist(network);
            if (i % 2 == 0) {
                permitted.add(network.getId());
            }
            networkIds.add(network.getId());
        }

        List<NetworkWithUsersAndDevicesVO> networks = networkDao.getNetworksByIdsAndUsers(null, networkIds, permitted);
        assertNotNull(networks);
        assertThat(networks, hasSize(permitted.size()));
        Set<Long> returned = networks.stream().map(NetworkVO::getId).collect(Collectors.toSet());
        assertThat(permitted, equalTo(returned));
    }

    @Test
    public void shouldListByNameWithSortingAndLimit() throws Exception {
        String name = RandomStringUtils.randomAlphabetic(10);
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setKey(RandomStringUtils.randomAlphabetic(10));
            if (i % 2 == 0) {
                network.setName(name);
            } else {
                network.setName(RandomStringUtils.randomAlphabetic(10));
            }
            network.setEntityVersion((long) i);
            networkDao.persist(network);
        }

        List<NetworkVO> networks = networkDao.list(name, null, "entityVersion", true, 10, 0, Optional.empty());
        assertThat(networks, hasSize(10));
        networks.forEach(n -> assertEquals(name, n.getName()));
        networks.stream().reduce((last, current) -> {
            if (last.getEntityVersion() > current.getEntityVersion())
                Assert.fail("Not sorted");
            return current;
        });
    }

}
