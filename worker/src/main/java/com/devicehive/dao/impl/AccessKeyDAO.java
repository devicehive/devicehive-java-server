package com.devicehive.dao.impl;

import com.devicehive.dao.IAccessKeyDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Created by tatyana on 3/16/15.
 */
@Repository
public class AccessKeyDAO implements IAccessKeyDAO {
    private static final String SELECT_QUERY = "SELECT id FROM access_key WHERE key = ?";

    protected JdbcTemplate jdbcTemplate;

    @Override
    public Long getAccessKeyId(String accessKey) {
        return jdbcTemplate.queryForObject(SELECT_QUERY, new Object[]{accessKey}, Long.class);
    }

    @Autowired
    @Qualifier("dbDataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}
