package com.devicehive.dao;

import com.devicehive.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * User: jkulagina
 * Date: 25.06.13
 * Time: 20:44
 */
public class ConfigurationDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;

    public Configuration getConfiguration(String name) {
        return em.find(Configuration.class, name);
    }

    @Transactional
    public void saveConfiguration(Configuration configuration){
        em.persist(configuration);
        em.flush();
    }

    @Transactional
    public void updateConfiguration(Configuration configuration){
        em.merge(configuration);
        em.flush();
    }
}
