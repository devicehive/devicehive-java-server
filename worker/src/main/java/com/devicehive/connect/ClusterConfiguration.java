package com.devicehive.connect;

import com.devicehive.domain.ClusterConfig;
import com.devicehive.domain.DeviceCommand;
import com.devicehive.domain.DeviceNotification;
import com.devicehive.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.cql.CqlIdentifier;
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
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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

    protected ClusterConfig clusterConfig;

    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected Environment environment;

    @Bean
    public CassandraClusterFactoryBean cluster() {
        final String contactPoints = clusterConfig.getCassandraContactpoints();
        final CassandraClusterFactoryBean cluster = new CassandraClusterFactoryBean();
        cluster.setContactPoints(contactPoints);
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
        return adminTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final String selectQuery = "SELECT value FROM configuration WHERE name = ?";
        final String metadataBrokerList = jdbcTemplate.queryForObject(selectQuery, new Object[]{Constants.METADATA_BROKER_LIST}, String.class);
        final String zookeeperConnect = jdbcTemplate.queryForObject(selectQuery, new Object[]{Constants.ZOOKEEPER_CONNECT}, String.class);
        final Integer threadsCount = jdbcTemplate.queryForObject(selectQuery, new Object[]{Constants.THREADS_COUNT}, Integer.class);
        final String cassandraContactpoints = jdbcTemplate.queryForObject(selectQuery, new Object[]{Constants.CASSANDRA_CONTACTPOINTS}, String.class);
        this.clusterConfig = new ClusterConfig(metadataBrokerList, zookeeperConnect, threadsCount, cassandraContactpoints);
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    @Autowired
    @Qualifier("dbDataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}