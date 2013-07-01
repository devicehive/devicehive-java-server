package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceNotification;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.sql.Date;

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
}
