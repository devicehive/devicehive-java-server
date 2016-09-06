package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.test.rule.KafkaEmbeddedRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.devicehive.shim.config.client.KafkaRpcClientConfig.RESPONSE_TOPIC;
import static com.devicehive.shim.config.server.KafkaRpcServerConfig.REQUEST_TOPIC;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
@TestPropertySource(locations={"classpath:application-test.properties", "classpath:application-test-configuration.properties"})
public abstract class AbstractSpringKafkaTest {

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

}
