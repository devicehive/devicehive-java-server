package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.UnknownHostException;


@Profile({"riak"})
@Configuration
public class RiakClusterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RiakClusterBuilder.class);

    private RiakCluster cluster;

    @Autowired
    private Environment env;

    @PostConstruct
    private void init() throws UnknownHostException {
        logger.debug("RiakClusterBuilder initialization started.");

        String riakHost = env.getProperty("riak.host");
        int riakPort = Integer.parseInt(env.getProperty("riak.port"));

        // This example will use only one node listening on localhost:10017
        RiakNode node = new RiakNode.Builder()
                .withRemoteAddress(riakHost)
                .withRemotePort(riakPort)
                .build();

        // This cluster object takes our one node as an argument
        cluster = new RiakCluster.Builder(node).build();

        // The cluster must be started to work, otherwise you will see errors
        cluster.start();

        logger.debug("RiakClusterBuilder initialization finished.");
    }

    @Bean
    @Lazy(false)
    public RiakClient riakClient() {
        return new RiakClient(cluster);
    }

    @Bean
    @Lazy(false)
    public RiakCluster riakCluster() {
        return cluster;
    }
}
