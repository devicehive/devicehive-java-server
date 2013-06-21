package com.devicehive.dao;

import com.devicehive.model.DeviceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

//import javax.transaction.Transactional;

/**
 * TODO JavaDoc
 */
@Stateful
public class DeviceClassDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;

    public List<DeviceClass> getList() {
        return em.createQuery("select dc from DeviceClass dc").getResultList();
    }

    public DeviceClass getDeviceClass(long id) {
        return em.find(DeviceClass.class, id);
    }

    @Transactional
    public long addDeviceClass(DeviceClass deviceClass) {
        em.persist(deviceClass);
        em.flush();
        return deviceClass.getId();
    }

    public void deleteDeviceClass(long id) {
        DeviceClass deviceClass = getDeviceClass(id);
        em.remove(deviceClass);
    }

}
