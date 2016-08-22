package com.devicehive.dao.rdbms;

import com.devicehive.dao.ConfigurationDao;
import com.devicehive.model.Configuration;
import com.devicehive.vo.ConfigurationVO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConfigurationDaoRdbmsImpl extends RdbmsGenericDao implements ConfigurationDao {

    @Override
    public Optional<ConfigurationVO> getByName(String name) {
        return Configuration.convert(createNamedQuery(Configuration.class, "Configuration.getByName", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst());
    }

    @Override
    public int delete(String name) {
        return createNamedQuery("Configuration.delete", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .executeUpdate();
    }

    @Override
    public void persist(ConfigurationVO configuration) {
        super.persist(Configuration.convert(configuration));
    }

    @Override
    public ConfigurationVO merge(ConfigurationVO existing) {
        return Configuration.convert(super.merge(Configuration.convert(existing)));
    }
}
