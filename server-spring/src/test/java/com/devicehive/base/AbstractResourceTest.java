package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DeviceHiveApplication.class)
@WebAppConfiguration
@IntegrationTest
public abstract class AbstractResourceTest {

    @Autowired
    private Environment env;
    protected String baseUri;
    protected RestTemplate template;

    @Before
    public void initSpringBootIntegrationTest() {
        this.baseUri = "http://localhost:" + env.getProperty("server.port") + "/rest";
        template = new TestRestTemplate();
    }

}
