package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;


/**
 * TODO JavaDoc
 */

public class DeviceClassDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public List<DeviceClass> getList() {
        return em.createQuery("select dc from DeviceClass dc").getResultList();
    }

    public DeviceClass getDeviceClass(long id) {
        return em.find(DeviceClass.class, id);
    }

    @Transactional
    public DeviceClass getDeviceClassByNameAndVersion(String name, String version){
        TypedQuery<DeviceClass> query = em.createNamedQuery("DeviceClass.findByNameAndVersion", DeviceClass.class);
        query.setParameter("version", version);
        query.setParameter("name", name);
        List<DeviceClass> result  = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional
    public void updateDeviceClass (DeviceClass deviceClass){
        em.lock(deviceClass, LockModeType.PESSIMISTIC_WRITE);
        em.merge(deviceClass);
    }

    @Transactional
    public long addDeviceClass(DeviceClass deviceClass) {
        em.persist(deviceClass);
        return deviceClass.getId();
    }

    public void deleteDeviceClass(long id) {
        DeviceClass deviceClass = getDeviceClass(id);
        em.remove(deviceClass);
    }

}
