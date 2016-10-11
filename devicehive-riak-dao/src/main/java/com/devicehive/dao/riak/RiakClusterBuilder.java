package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
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
import java.util.HashMap;
import java.util.Map;


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

    @Bean
    @Lazy(false)
    public RiakQuorum riakQuorum(@Value("${riak.read-quorum-option:r}") String rqOpt,
                                 @Value("${riak.read-quorum:default}") String rq,
                                 @Value("${riak.write-quorum.option:w}") String wqOpt,
                                 @Value("${riak.write-quorum:default}") String wq) {
        Map<String, FetchValue.Option<Quorum>> readOptions = new HashMap<String, FetchValue.Option<Quorum>>() {{
            put("r", FetchValue.Option.R);
            put("pr", FetchValue.Option.PR);
        }};
        Map<String, StoreValue.Option<Quorum>> writeOptions = new HashMap<String, StoreValue.Option<Quorum>>() {{
            put("w", StoreValue.Option.W);
            put("pw", StoreValue.Option.PW);
            put("dw", StoreValue.Option.DW);
        }};
        Map<String, Quorum> quorums = new HashMap<String, Quorum>() {{
            put("one", Quorum.oneQuorum());
            put("all", Quorum.allQuorum());
            put("quorum", Quorum.quorumQuorum());
            put("default", Quorum.defaultQuorum());
        }};

        FetchValue.Option<Quorum> readQuorumOption = readOptions.get(rqOpt);
        Quorum readQuorum = quorums.get(rq);

        StoreValue.Option<Quorum> writeQuorumOption = writeOptions.get(wqOpt);
        Quorum writeQuorum = quorums.get(wq);


        return new RiakQuorum(readQuorumOption, readQuorum, writeQuorumOption, writeQuorum);
    }
}
