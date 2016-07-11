package com.devicehive.dao.riak;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.ConfigurationDao;
import com.devicehive.model.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Profile({"riak"})
@Repository
public class ConfigurationDaoImpl extends GenericDaoImpl implements ConfigurationDao {

    @Override
    public Optional<Configuration> getByName(String name) {
        return createNamedQuery(Configuration.class, "Configuration.getByName", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst();
    }

    @Override
    public int delete(String name) {
        return createNamedQuery("Configuration.delete", Optional.<CacheConfig>empty())
                .setParameter("name", name)
                .executeUpdate();
    }

    @Override
    public Configuration find(Long id) {
        return find(Configuration.class, id);
    }

    @Override
    public void persist(Configuration configuration) {
        super.persist(configuration);
    }

    @Override
    public Configuration merge(Configuration existing) {
        return super.merge(existing);
    }
}
