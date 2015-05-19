package com.devicehive.configuration;

import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.model.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.validation.constraints.NotNull;

@Singleton
@Startup
public class ConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationService.class);

    @EJB
    private ConfigurationDAO configurationDAO;

    public <T> void save(@NotNull String name, T value) {
        String str = value != null ? value.toString() : null;
        configurationDAO.save(name, str);
    }

    public String get(@NotNull String name) {
        Configuration configuration = configurationDAO.findByName(name);
        if (configuration == null) {
            LOGGER.warn(String.format(Messages.CONFIG_NOT_FOUND, name));
            return null;
        }
        return configuration.getValue();
    }

    public long getLong(@NotNull String name, long defaultValue) {
        String val = get(name);
        return val != null ? Long.parseLong(val) : defaultValue;
    }

    public int getInt(@NotNull String name, int defaultValue) {
        String val = get(name);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public boolean getBoolean(@NotNull String name, boolean defaultValue) {
        String val = get(name);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public <T> void delete(@NotNull String name) {
        configurationDAO.delete(name);
    }

}
