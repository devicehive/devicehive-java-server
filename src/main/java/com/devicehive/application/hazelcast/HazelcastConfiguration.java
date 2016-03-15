package com.devicehive.application.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class HazelcastConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfiguration.class);

    private static final String INSTANCE_NAME = "DeviceHiveInstance";

    @Autowired
    private Environment env;

    @Bean(destroyMethod = "shutdown")
    @Lazy(value = false)
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public HazelcastInstance hazelcast(Config config) {
        logger.debug("Creating new Hazelcast instance {}", INSTANCE_NAME);
        config.setInstanceName(INSTANCE_NAME);
        config.setManagedContext(hzSpringManagedContext());
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        logger.info("Initializing Hazelcast is complete");
        return instance;
    }

    @Bean
    public SpringManagedContext hzSpringManagedContext() {
        return new SpringManagedContext();
    }

    @Bean
    public Config config(NetworkConfig networkConfig) {
        final Config config = new XmlConfigBuilder().build();
        config.setNetworkConfig(networkConfig);
        config.getGroupConfig().setName(env.getProperty("hazelcast.group.name"));

        return config;
    }

    @Bean
    public NetworkConfig networkConfig(@Value("${hazelcast.port}") int port, JoinConfig joinConfig) {
        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setJoin(joinConfig);
        networkConfig.setPort(port);
        return networkConfig;
    }

    @Bean
    public JoinConfig joinConfig(TcpIpConfig tcpIpConfig) {
        final JoinConfig joinConfig = disabledMulticast();
        joinConfig.setTcpIpConfig(tcpIpConfig);
        return joinConfig;
    }


    @Bean
    public TcpIpConfig tcpIpConfig(ServiceDiscovery<Void> serviceDiscovery) throws Exception {
        final TcpIpConfig tcpIpConfig = new TcpIpConfig();
        final List<String> instances = queryOtherInstancesInZk(env.getProperty("curator.service"), serviceDiscovery);
        tcpIpConfig.setMembers(instances);
        tcpIpConfig.setEnabled(true);
        return tcpIpConfig;
    }

    private JoinConfig disabledMulticast() {
        final JoinConfig join = new JoinConfig();
        final MulticastConfig multicastConfig = new MulticastConfig();
        multicastConfig.setEnabled(false);
        join.setMulticastConfig(multicastConfig);
        return join;
    }

    private List<String> queryOtherInstancesInZk(String name, ServiceDiscovery<Void> serviceDiscovery) throws Exception {
        return serviceDiscovery
                .queryForInstances(name)
                .stream()
                .map(ServiceInstance::buildUriSpec)
                .collect(Collectors.toList());
    }
}