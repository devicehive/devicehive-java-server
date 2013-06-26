package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 19.06.13
 * Time: 16:08
 * To change this template use File | Settings | File Templates.
 */
public class DeviceDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public Device findById(Long id) {
        return em.find(Device.class, id);
    }

    @Transactional
    public Device findByUUID(UUID uuid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUID", Device.class);
        query.setParameter("uuid", uuid);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }


    @Transactional
    public Device findByUUIDAndKey(UUID uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndKey", Device.class);
        query.setParameter("uuid", uuid);
        query.setParameter("key", key);
        return query.getSingleResult();
    }

    @Transactional
    public void updateDevice(Device device) {
//        em.refresh(device, LockModeType.PESSIMISTIC_WRITE);
        em.merge(device);
        em.flush();
    }

    @Transactional
    public void registerDevice(Device device) {
//        em.refresh(device, LockModeType.PESSIMISTIC_WRITE);
        em.persist(device);
        em.flush();
    }
    @Transactional
    public List<Device> findByUUIDAndUser(User user, List<UUID> list){
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guidList", list);
        return query.getResultList();
    }


}
