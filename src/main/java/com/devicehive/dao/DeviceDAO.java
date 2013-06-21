package com.devicehive.dao;

import com.devicehive.model.Device;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 19.06.13
 * Time: 16:08
 * To change this template use File | Settings | File Templates.
 */
public class DeviceDAO {


    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;

    @Transactional
    public Device findById(Long id) {
        return em.find(Device.class, id);
    }

    @Transactional
    public Device findByUUID(UUID uuid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUID", Device.class);
        query.setParameter("uuid", uuid);
        return query.getSingleResult();
    }

    @Transactional
    public Device findByUUIDAndKey(UUID uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndKey", Device.class);
        query.setParameter("uuid", uuid);
        query.setParameter("key", key);
        return query.getSingleResult();
    }


}
