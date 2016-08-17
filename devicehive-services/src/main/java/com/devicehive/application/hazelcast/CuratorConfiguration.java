package com.devicehive.application.hazelcast;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class CuratorConfiguration {

    @Bean(initMethod = "start", destroyMethod = "close")
    ServiceDiscovery<Void> serviceDiscovery(CuratorFramework curatorFramework, ServiceInstance<Void> serviceInstance) throws Exception {
        return ServiceDiscoveryBuilder
                .builder(Void.class)
                .basePath("hazelcast")
                .client(curatorFramework)
                .thisInstance(serviceInstance)
                .build();
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    CuratorFramework curatorFramework(@Value("${zookeeper.connect}") String zooKeeperUrl) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        return CuratorFrameworkFactory.newClient(zooKeeperUrl, retryPolicy);
    }

    @Bean
    ServiceInstance<Void> serviceInstance(@Value("${hazelcast.port}") int port, ApplicationContext applicationContext,
                                          CuratorFramework curatorFramework) throws Exception {
        final String hostName = InetAddress.getLocalHost().getHostName();
        return ServiceInstance
                .<Void>builder()
                .name(applicationContext.getId())
                .uriSpec(new UriSpec("{address}:{port}"))
                .address(hostName)
                .port(port)
                .build();
    }
}
