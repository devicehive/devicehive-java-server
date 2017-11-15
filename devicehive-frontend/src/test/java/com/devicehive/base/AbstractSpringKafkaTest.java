package com.devicehive.base;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.application.DeviceHiveFrontendApplication;
import com.devicehive.test.rule.KafkaEmbeddedRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
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
@SpringBootTest(classes = {DeviceHiveFrontendApplication.class})
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
                    .map(mac -> Base64.getEncoder().withoutPadding().encodeToString(mac)).orElse(UUID.randomUUID().toString());
            prefix = prefix.replace("+", "")
                    .replace("/", "")
                    .replace("=", "");
            RESPONSE_TOPIC = "response_topic_" + prefix;
        } catch (SocketException | UnknownHostException e) {
            RESPONSE_TOPIC = "response_topic_" + UUID.randomUUID().toString();
        }
    }
    @ClassRule
    public static KafkaEmbeddedRule kafkaRule = new KafkaEmbeddedRule(true, 5, REQUEST_TOPIC, RESPONSE_TOPIC);

    @Rule
    public Timeout testTimeout = new Timeout(180000, TimeUnit.MILLISECONDS); // 180k ms = 3 minutes

    @Before
    public void setUp() throws Exception {
        // FIXME: HACK! We must find a better solution to postpone test execution until all components (shim, kafka, etc) will be ready
        TimeUnit.SECONDS.sleep(10);
    }
}
