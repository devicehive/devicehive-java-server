package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.resource.converters.CollectionProvider;
import com.devicehive.resource.converters.HiveEntityProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.Base64;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DeviceHiveApplication.class)
@WebAppConfiguration
@IntegrationTest
//@TestPropertySource("classpath:application-test.properties")
public abstract class AbstractResourceTest {
    public static final String ADMIN_LOGIN = "test_admin";
    public static final String ADMIN_PASS = "admin_pass";

    @ClassRule
    public static EmbeddedKafkaClusterRule kafkaClusterRule = new EmbeddedKafkaClusterRule();

    @ClassRule
    public static EmbeddedRedisRule redisRule = new EmbeddedRedisRule();

    @Autowired
    private Environment env;

    private String baseUri;
    private WebTarget target;

    @Before
    public void initSpringBootIntegrationTest() {
        baseUri = "http://localhost:" + env.getProperty("server.port");
        Client client = ClientBuilder.newClient();
        client.register(HiveEntityProvider.class);
        client.register(CollectionProvider.class);
        target = client.target(baseUri).path("rest");
    }

    protected WebTarget target() {
        return target;
    }

    protected String baseUri() {
        return baseUri;
    }

    protected String basicAuthHeader(String login, String password) {
        String str = String.format("%s:%s", login, password);
        String base64 = Base64.getEncoder().encodeToString(str.getBytes());
        return String.format("Basic %s", base64);
    }
}
