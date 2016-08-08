package com.devicehive.configuration;

import com.devicehive.dao.ConfigurationDao;
import com.devicehive.vo.ConfigurationVO;
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
    private ConfigurationDao configurationDao;

    @Transactional
    public <T> void save(@NotNull String name, T value) {
        //TODO check keys are same
        String str = value != null ? value.toString() : null;
        Optional<ConfigurationVO> existingOpt = findByName(name);
        if (existingOpt.isPresent()) {
            ConfigurationVO existing = existingOpt.get();
            existing.setValue(str);
            configurationDao.merge(existing);
        } else {
            ConfigurationVO configuration = new ConfigurationVO();
            configuration.setName(name);
            configuration.setValue(str);
            configurationDao.persist(configuration);
        }
    }

    private Optional<ConfigurationVO> findByName(String name) {
        return configurationDao.getByName(name);
    }

    public String get(@NotNull String name) {
        return findByName(name)
                .map(ConfigurationVO::getValue)
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
        int result = configurationDao.delete(name);
        logger.info("Deleted {} configuration entries by name {}", result, name);
    }

}
