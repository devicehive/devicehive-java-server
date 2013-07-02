package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

public class DeviceNotificationDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public void saveNotification(DeviceNotification deviceNotification) {
        try {
            deviceNotification.setTimestamp(new Date(System.currentTimeMillis()));
            em.persist(deviceNotification);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @Transactional
    public DeviceNotification findById(Long id) {
        return em.find(DeviceNotification.class, id);
    }

    @Transactional
    public List<DeviceNotification> findByDevicesNewerThan(List<Device> deviceList, Date timestamp){
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByDeviceNewerThan",
                DeviceNotification.class);
        query.setParameter("deviceList", deviceList);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @Transactional
    public List<DeviceNotification> findNewerThan(Date timestamp){
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByNewerThan",
                DeviceNotification.class);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }


}
