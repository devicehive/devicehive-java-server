package com.devicehive.connect;

import com.devicehive.domain.ClusterConfig;
import com.devicehive.utils.Constants;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Created by tmatvienko on 2/6/15.
 */
@Configuration
@PropertySource(value = {"classpath:app.properties"})
@EnableCassandraRepositories(basePackages = {"com.devicehive"})
public class TestCassandraConfiguration extends ClusterConfiguration {

    @Override
    protected String getKeyspaceName() {
        return environment.getProperty(Constants.CASSANDRA_KEYSPACE_TEST);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.clusterConfig = new ClusterConfig(null, null, null, "127.0.0.1");
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}
