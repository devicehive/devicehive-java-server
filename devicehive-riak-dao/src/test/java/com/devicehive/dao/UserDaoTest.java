package com.devicehive.dao;

import com.devicehive.dao.riak.UserNetworkDaoRiakImpl;
import com.devicehive.model.User;
import com.devicehive.dao.riak.model.UserNetwork;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.NetworkVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class UserDaoTest extends AbstractResourceTest {

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
        User user = new User();
        user.setId(100L);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        userDao.persist(user);

        User newUser = userDao.find(100L);
        assertNotNull(newUser);
    }

    @Test
    public void testDelete() throws Exception {
        User user = new User();
        user.setId(100L);
        userDao.persist(user);

        userDao.deleteById(100L);

        User newUser = userDao.find(100L);
        assertNull(newUser);
    }

    @Test
    public void testMerge() throws Exception {
        User user = new User();
        user.setId(100L);
        user.setLogin("before merge");
        userDao.persist(user);

        user.setLogin("after merge");
        userDao.merge(user);

        User newUser = userDao.find(100L);
        assertEquals("after merge", newUser.getLogin());
    }

    public void testFindByName() throws Exception {
        User user = new User();
        Long id = 100L;
        user.setId(id);
        user.setLogin("login");
        userDao.persist(user);

        Optional<User> newUser = userDao.findByName("login");
        assertTrue(newUser.isPresent());
        assertEquals(id, newUser.get().getId());
    }

    @Test
    public void testFindByGoogleName() throws Exception {
        User user = new User();
        Long id = 100L;
        user.setId(id);
        user.setGoogleLogin("google login");
        userDao.persist(user);

        User newUser = userDao.findByGoogleName("google login");
        assertEquals(id, newUser.getId());
    }

    @Test
    public void testFindByFacebookName() throws Exception {
        User user = new User();
        Long id = 100L;
        user.setId(id);
        user.setFacebookLogin("facebook login");
        userDao.persist(user);

        User newUser = userDao.findByFacebookName("facebook login");
        assertEquals(id, newUser.getId());
    }

    @Test
    public void testFindByGithubName() throws Exception {
        User user = new User();
        Long id = 100L;
        user.setId(id);
        user.setGithubLogin("github login");
        userDao.persist(user);

        User newUser = userDao.findByGithubName("github login");
        assertEquals(id, newUser.getId());
    }

    @Test
    public void testFindByIdentityName() throws Exception {
        User user = new User();
        Long id = 100L;
        user.setId(id);
        user.setLogin("login");
        user.setGoogleLogin("google login");
        user.setFacebookLogin("facebook login");
        user.setGithubLogin("github login");
        user.setStatus(UserStatus.ACTIVE);
        userDao.persist(user);

        Optional<User> emptyUser = userDao.findByIdentityName("login", "google login", "facebook login", "github login");
        assertFalse(emptyUser.isPresent());

        Optional<User> presentGoogle = userDao.findByIdentityName("l", "google login", "", "");
        assertTrue(presentGoogle.isPresent());

        Optional<User> presentFacebook = userDao.findByIdentityName("l", "", "facebook login", "");
        assertTrue(presentFacebook.isPresent());

        Optional<User> presentGithub = userDao.findByIdentityName("l", "", "", "github login");
        assertTrue(presentGithub.isPresent());
    }

    @Test
    public void testHasAccessToNetwork() throws Exception {
        User user = new User();
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
        User user = new User();
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

        Set userNetworks = userNetworkDao.findNetworksForUser(id);
        assertFalse(userNetworks.contains(64L));
        assertTrue(userNetworks.contains(65L));
    }
}
