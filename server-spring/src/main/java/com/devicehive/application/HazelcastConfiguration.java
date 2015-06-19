package com.devicehive.application;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class HazelcastConfiguration {

    @Autowired
    private Environment env;

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcast(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public Config config(NetworkConfig networkConfig) {
        final Config config = new Config();
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
    public TcpIpConfig tcpIpConfig(ServiceDiscovery<Void> serviceDiscovery, ApplicationContext context) throws Exception {
        final TcpIpConfig tcpIpConfig = new TcpIpConfig();
        final List<String> instances = queryOtherInstancesInZk(context.getId(), serviceDiscovery);
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