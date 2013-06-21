package com.devicehive.dao;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 20.06.13
 * Time: 15:36
 * To change this template use File | Settings | File Templates.
 */
public class DeviceNotificationDAO {

    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;


    @Transactional
    public void saveNotification(DeviceNotification deviceNotification) {
        em.persist(deviceNotification);
    }


    @Transactional
    public DeviceNotification findById(Long id) {
        return em.find(DeviceNotification.class, id);
    }
}
