package com.devicehive.service;

import com.devicehive.dao.IAccessKeyDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Created by tatyana on 3/16/15.
 */
@Service
public class AuthenticationService {
    protected JdbcTemplate jdbcTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private IAccessKeyDAO accessKeyDAO;

    @Cacheable("accessKeys")
    public Long authenticate(String accessKey) {
        try {
            return accessKeyDAO.getAccessKeyId(accessKey);
        } catch (EmptyResultDataAccessException ex) {
            LOGGER.error("Access key {} not found. Authentication failed.", accessKey);
            return null;
        }
    }

    @Autowired
    @Qualifier("dbDataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}
