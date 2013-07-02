package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceEquipment;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

public class DeviceEquipmentDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public void saveDeviceEquipment(DeviceEquipment deviceEquipment) {
        deviceEquipment.setTimestamp(new Date(System.currentTimeMillis()));
        em.persist(deviceEquipment);
    }

    @Transactional
    public DeviceEquipment findById(Long id) {
        return em.find(DeviceEquipment.class, id);
    }

    @Transactional
    public DeviceEquipment findByCode(String code) {
        TypedQuery<DeviceEquipment> query = em.createNamedQuery("DeviceEquipment.getByCode", DeviceEquipment.class);
        query.setParameter("code", code);
        List<DeviceEquipment> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @Transactional
    public void updateDeviceEquipment(DeviceEquipment deviceEquipment) {
        deviceEquipment.setTimestamp(new Date(System.currentTimeMillis()));
        em.lock(deviceEquipment, LockModeType.PESSIMISTIC_WRITE);
        em.merge(deviceEquipment);
    }
}
