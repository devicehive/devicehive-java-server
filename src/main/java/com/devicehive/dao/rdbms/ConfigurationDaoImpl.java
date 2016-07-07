package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.ConfigurationDao;
import com.devicehive.model.Configuration;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConfigurationDaoImpl extends GenericDaoImpl implements ConfigurationDao {

    public Optional<Configuration> getByName(String name) {
        return createNamedQuery(Configuration.class, "Configuration.getByName", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst();
    }

    public int delete(String name) {
        return createNamedQuery("Configuration.delete", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .executeUpdate();
    }
}
