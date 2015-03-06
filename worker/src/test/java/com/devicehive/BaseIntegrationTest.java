package com.devicehive;

import com.devicehive.connect.TestCassandraConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.UUID;

/**
 * Created by tmatvienko on 2/2/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestCassandraConfiguration.class)
public class BaseIntegrationTest {
    final Date date = new Date();
    final String deviceGuid = UUID.randomUUID().toString();
    final String deviceGuid2 = UUID.randomUUID().toString();
    final String deviceGuid3 = UUID.randomUUID().toString();

    @Autowired
    private CassandraAdminOperations adminTemplate;

    @Test
    public void test() {}
}