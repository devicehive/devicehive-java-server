package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;


@Stateless
public class ConfigurationDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Configuration findByName(@NotNull String name) {
        return em.find(Configuration.class, name);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Configuration> findAll(){
        TypedQuery<Configuration> query = em.createNamedQuery("Configuration.getAll", Configuration.class);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void save(String name, String value){
        Query query = em.createNamedQuery("Configuration.update");
        query.setParameter("name", name);
        query.setParameter("value", value);
        if (query.executeUpdate() == 0){
            em.persist(new Configuration(name, value));
        }
    }
}
