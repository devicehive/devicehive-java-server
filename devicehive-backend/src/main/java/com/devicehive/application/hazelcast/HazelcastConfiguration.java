package com.devicehive.application.hazelcast;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.model.DevicePortableFactory;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Configuration
@PropertySource("classpath:application.properties")
public class HazelcastConfiguration {

    @Value("${hazelcast.group.name}")
    private String groupName;
    @Value("${hazelcast.group.password}")
    private String groupPassword;
    @Value("#{'${hazelcast.cluster.members}'.split(',')}")
    private List<String> clusterMembers;
    @Value("${hazelcast.client.event.thread.count:5}")
    private String eventThreadCount;

    @Bean
    public HazelcastInstance hazelcast() throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig()
                .setName(groupName)
                .setPassword(groupPassword);
        clientConfig.getNetworkConfig()
                .setAddresses(clusterMembers);
        clientConfig.getSerializationConfig()
                .addPortableFactory(1, new DevicePortableFactory());
        clientConfig.setProperty("hazelcast.client.event.thread.count", eventThreadCount);

        return HazelcastClient.newHazelcastClient(clientConfig);
    }
}
