package com.devicehive.application;

import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphOptions;
import com.datastax.dse.graph.api.DseGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
@EnableAutoConfiguration(exclude = {JacksonAutoConfiguration.class,
        DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class})
@PropertySource("classpath:application-persistence.properties")
public class GraphClusterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GraphClusterConfiguration.class);

    private GraphTraversalSource g;

    @Autowired
    private Environment env;

    @PostConstruct
    private void init() {
        logger.debug("Graph initialization started.");

        String host = env.getProperty("graph.host");
        String graphName = env.getProperty("graph.name");

        DseCluster dseCluster = DseCluster.builder()
                .addContactPoint(host)
                .withGraphOptions(new GraphOptions()
                        .setGraphName(graphName))
                .build();
        DseSession dseSession = dseCluster.connect();

        g = DseGraph.traversal(dseSession);

        logger.debug("Graph initialization finished.");
    }


}
