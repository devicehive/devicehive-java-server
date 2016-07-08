package com.devicehive.dao.riak;

import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.UnknownHostException;


/**
 * Created by Gleb on 08.07.2016.
 */

@Profile("riak")
@Component
public class RiakClusterBuilder {

    @PostConstruct
    private void init() {

    }

    RiakCluster setUpCluster() throws UnknownHostException {
        // This example will use only one node listening on localhost:10017
        RiakNode node = new RiakNode.Builder()
                .withRemoteAddress("127.0.0.1")
                .withRemotePort(10017)
                .build();

        // This cluster object takes our one node as an argument
        RiakCluster cluster = new RiakCluster.Builder(node)
                .build();

        // The cluster must be started to work, otherwise you will see errors
        cluster.start();

        return cluster;
    }
}
