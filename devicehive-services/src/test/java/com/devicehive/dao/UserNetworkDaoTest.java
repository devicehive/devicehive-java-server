package com.devicehive.dao;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.dao.riak.UserNetworkDaoRiakImpl;
import com.devicehive.model.UserNetwork;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UserNetworkDaoTest extends AbstractResourceTest {

    @Autowired(required = false)
    UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    ApplicationContext ctx;

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(ctx.getEnvironment().acceptsProfiles("riak"));
    }

    @Test
    public void testRetrieval() throws Exception {
        UserNetwork un1 = new UserNetwork();
        un1.setUserId(1L);
        un1.setNetworkId(64L);
        userNetworkDao.persist(un1);

        UserNetwork un2 = new UserNetwork();
        un2.setUserId(1L);
        un2.setNetworkId(65L);
        userNetworkDao.persist(un2);

        UserNetwork un3 = new UserNetwork();
        un3.setUserId(2L);
        un3.setNetworkId(64L);
        userNetworkDao.persist(un3);

        Set<Long> networks1 = userNetworkDao.findNetworksForUser(1L);
        assertNotNull(networks1);
        assertTrue(networks1.contains(64L));
        assertTrue(networks1.contains(65L));

        Set<Long> users1 = userNetworkDao.findUsersInNetwork(64L);
        assertNotNull(users1);
        assertTrue(users1.contains(1L));
        assertTrue(users1.contains(2L));
    }
}
