package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

public class ConfigurationDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public Configuration findByName(String name) {
        return em.find(Configuration.class, name);
    }

    @Transactional
    public void saveConfiguration(Configuration configuration){
        em.persist(configuration);
    }

    @Transactional
    public void updateConfiguration(Configuration configuration){
        em.merge(configuration);
    }
}
