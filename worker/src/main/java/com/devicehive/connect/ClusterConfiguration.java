package com.devicehive.connect;

import com.datastax.driver.core.PlainTextAuthProvider;
import com.devicehive.domain.ClusterConfig;
import com.devicehive.domain.DeviceCommand;
import com.devicehive.domain.DeviceNotification;
import com.devicehive.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.cassandra.core.keyspace.CreateIndexSpecification;
import org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tmatvienko on 2/5/15.
 */
@Configuration
@PropertySource(value = {"classpath:app.properties"})
@EnableCassandraRepositories(basePackages = {"com.devicehive.repository"})
public class ClusterConfiguration extends AbstractCassandraConfiguration implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterConfiguration.class);

    public static final String METADATA_BROKER_LIST = "metadata.broker.list";
    public static final String ZOOKEEPER_CONNECT = "zookeeper.connect";
    public static final String THREADS_COUNT = "threads.count";
    public static final String CASSANDRA_CONTACTPOINTS = "cassandra.contactpoints";

    protected ClusterConfig clusterConfig;

    @Autowired
    protected Environment environment;

    @Override
    public CassandraClusterFactoryBean cluster() {
        final String contactPoints = clusterConfig.getCassandraContactpoints();
        final CassandraClusterFactoryBean cluster = new CassandraClusterFactoryBean();
        cluster.setContactPoints(contactPoints);
        cluster.setAuthProvider(new PlainTextAuthProvider(environment.getProperty(Constants.CASSANDRA_USERNAME),
                environment.getProperty(Constants.CASSANDRA_PASSWORD)));
        List<CreateKeyspaceSpecification> createKeyspaceSpecifications = Arrays.asList(
                new CreateKeyspaceSpecification(environment.getProperty(Constants.CASSANDRA_KEYSPACE)).ifNotExists(),
                new CreateKeyspaceSpecification(environment.getProperty(Constants.CASSANDRA_KEYSPACE_TEST)).ifNotExists()
        );
        cluster.setKeyspaceCreations(createKeyspaceSpecifications);
        LOGGER.info("Cassandra cluster started on {}", contactPoints);
        return cluster;
    }

    @Override
    protected String getKeyspaceName() {
        return environment.getProperty(Constants.CASSANDRA_KEYSPACE);
    }

    @Bean
    public CassandraMappingContext cassandraMapping() throws ClassNotFoundException {
        return new BasicCassandraMappingContext();
    }

    @Bean
    public CassandraAdminOperations cassandraTemplate() throws Exception {
        CassandraAdminOperations adminTemplate = new CassandraAdminTemplate(this.session().getObject(), this.cassandraConverter());
        adminTemplate.createTable(true, CqlIdentifier.cqlId("device_notification"), DeviceNotification.class,
                new HashMap<String, Object>());
        adminTemplate.createTable(true, CqlIdentifier.cqlId("device_command"), DeviceCommand.class,
                new HashMap<String, Object>());
        //adminTemplate.execute(new CreateIndexSpecification().tableName("device_command").columnName("device_guid").ifNotExists());
        adminTemplate.execute(new CreateIndexSpecification().tableName("device_command").columnName("command").ifNotExists());
        return adminTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final String metadataBrokerList = environment.getProperty(METADATA_BROKER_LIST);
        final String zookeeperConnect = environment.getProperty(ZOOKEEPER_CONNECT);
        final Integer threadsCount = Integer.parseInt(environment.getProperty(THREADS_COUNT));
        final String cassandraContactpoints = environment.getProperty(CASSANDRA_CONTACTPOINTS);
        this.clusterConfig = new ClusterConfig(metadataBrokerList, zookeeperConnect, threadsCount, cassandraContactpoints);
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }
}