package com.devicehive.configuration;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.GenericDAO;
import com.devicehive.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Component
@Lazy(false)
public class ConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private GenericDAO genericDAO;

    @Transactional
    public <T> void save(@NotNull String name, T value) {
        String str = value != null ? value.toString() : null;
        Optional<Configuration> existingOpt = findByName(name);
        if (existingOpt.isPresent()) {
            Configuration existing = existingOpt.get();
            existing.setValue(str);
            genericDAO.merge(existing);
        } else {
            Configuration configuration = new Configuration();
            configuration.setName(name);
            configuration.setValue(str);
            genericDAO.persist(configuration);
        }
    }

    private Optional<Configuration> findByName(String name) {
        return genericDAO.createNamedQuery(Configuration.class, "Configuration.getByName", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst();
    }

    public String get(@NotNull String name) {
        return findByName(name)
                .map(Configuration::getValue)
                .orElseGet(() -> {
                    logger.warn(String.format(Messages.CONFIG_NOT_FOUND, name));
                    return null;
                });
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

    @Transactional
    public <T> void delete(@NotNull String name) {
        int result = genericDAO.createNamedQuery("Configuration.delete", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .executeUpdate();
        logger.info("Deleted {} configuration entries by name {}", result, name);
    }

}
