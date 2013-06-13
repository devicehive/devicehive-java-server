package com.devicehive.dao;

import com.devicehive.model.DeviceClass;

import javax.enterprise.inject.Model;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
//import javax.transaction.Transactional;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Model
public class DeviceClassDAO {
    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;

    public List<DeviceClass> getList() {
        return em.createQuery("select dc from DeviceClass dc").getResultList();
    }

    public void addDeviceClass(DeviceClass deviceClass) {
        em.persist(deviceClass);
    }

    public void deleteDeviceClass(DeviceClass deviceClass) {
        em.remove(deviceClass);
    }
}
