package com.devicehive.dao.impl;

import com.devicehive.dao.IConfigurationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Created by tatyana on 3/16/15.
 */
@Repository
public class ConfigurationDAO implements IConfigurationDAO {
    private static final String SELECT_QUERY = "SELECT value FROM configuration WHERE name = ?";

    protected JdbcTemplate jdbcTemplate;

    @Override
    public String getStringConfig(String configName) {
        return jdbcTemplate.queryForObject(SELECT_QUERY, new Object[]{configName}, String.class);
    }

    @Override
    public Integer getIntegerConfig(String configName) {
        return jdbcTemplate.queryForObject(SELECT_QUERY, new Object[]{configName}, Integer.class);
    }

    @Autowired
    @Qualifier("dbDataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}
