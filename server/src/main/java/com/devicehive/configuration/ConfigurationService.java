package com.devicehive.configuration;

import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.validation.constraints.NotNull;

@Singleton
@Startup
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    @EJB
    private ConfigurationDAO configurationDAO;


    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T> void save(@NotNull String name, T value) {
        String str = value != null ? value.toString() : null;
        configurationDAO.save(name, str);
    }


    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String get(@NotNull String name) {
        Configuration configuration = configurationDAO.findByName(name);
        return configuration != null ? configuration.getValue() : null;
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getLong(@NotNull String name, long defaultValue) {
        String val = get(name);
        return val != null ? Long.parseLong(val) : defaultValue;
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int getInt(@NotNull String name, int defaultValue) {
        String val = get(name);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean getBoolean(@NotNull String name, boolean defaultValue) {
        String val = get(name);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

}
