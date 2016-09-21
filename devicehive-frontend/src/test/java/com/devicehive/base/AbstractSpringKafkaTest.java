package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.test.rule.KafkaEmbeddedRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
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


@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest
@SpringApplicationConfiguration(classes = {DeviceHiveApplication.class})
@TestPropertySource(locations={"classpath:application-test.properties", "classpath:application-test-configuration.properties"})
public abstract class AbstractSpringKafkaTest {
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
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 5, REQUEST_TOPIC, RESPONSE_TOPIC);
}
