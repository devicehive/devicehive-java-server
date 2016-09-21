package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.test.rule.KafkaEmbeddedRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@DirtiesContext
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
@TestPropertySource(locations={"classpath:application-test.properties", "classpath:application-test-configuration.properties"})
public abstract class AbstractSpringTest {

    public static String RESPONSE_TOPIC;
    public static final String REQUEST_TOPIC = "request_topic";

    static {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            String prefix = Optional.ofNullable(ni)
                    .map(n -> {
                        try {
                            return n.getHardwareAddress();
                        } catch (SocketException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(mac -> Base64.getEncoder().encodeToString(mac)).orElse(UUID.randomUUID().toString());
            RESPONSE_TOPIC = "response_topic_" + prefix;
        } catch (SocketException | UnknownHostException e) {
            RESPONSE_TOPIC = "response_topic_" + UUID.randomUUID().toString();
        }
    }

    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 1, REQUEST_TOPIC, RESPONSE_TOPIC);

    @Before
    public void setUp() throws Exception {
        // FIXME: HACK! We must find a better solution to postpone test execution until all components (shim, kafka, etc) will be ready
        TimeUnit.SECONDS.sleep(10);
    }
}
