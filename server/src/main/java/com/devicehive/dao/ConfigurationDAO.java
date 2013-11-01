package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Configuration;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;


@Stateless
public class ConfigurationDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Configuration findByName(@NotNull String name) {
        return em.find(Configuration.class, name);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Configuration> findAll() {
        TypedQuery<Configuration> query = em.createNamedQuery("Configuration.getAll", Configuration.class);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void save(@NotNull String name, String value) {
        Configuration existing = findByName(name);
        if (existing != null) {
            existing.setValue(value);
        } else {
            Configuration configuration = new Configuration();
            configuration.setName(name);
            configuration.setValue(value);
            em.persist(configuration);
        }
    }
}
