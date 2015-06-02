package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DeviceHiveApplication.class)
@TestPropertySource("classpath:application.properties")
@WebAppConfiguration
@IntegrationTest
public abstract class AbstractResourceTest {

    @ClassRule
    public static EmbeddedKafkaClusterRule kafkaClusterRule = new EmbeddedKafkaClusterRule();

    @ClassRule
    public static EmbeddedRedisRule redisRule = new EmbeddedRedisRule();

    @Autowired
    private Environment env;

    protected static String baseUri;
    protected static WebTarget target;

    @Before
    public void initSpringBootIntegrationTest() {
        baseUri = "http://localhost:" + env.getProperty("server.port");
        Client client = ClientBuilder.newClient();
        target = client.target(baseUri).path("rest");
    }

}
