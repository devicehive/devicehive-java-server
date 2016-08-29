package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.rule.KafkaEmbeddedRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static com.devicehive.shim.config.client.KafkaRpcClientConfig.RESPONSE_TOPIC;
import static com.devicehive.shim.config.server.KafkaRpcServerConfig.REQUEST_TOPIC;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
@TestPropertySource(locations={"classpath:application-test.properties", "classpath:application-test-configuration.properties"})
public abstract class AbstractSpringTest {

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    @Before
    public void setUp() throws Exception {
        // FIXME: HACK! We must find a better solution to postpone test execution until all components (shim, kafka, etc) will be ready
        TimeUnit.SECONDS.sleep(10);
    }
}
