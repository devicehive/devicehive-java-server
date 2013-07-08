package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DeviceDAO {

    private static Logger logger = LoggerFactory.getLogger(DeviceDAO.class);
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
            em.lock(device, LockModeType.PESSIMISTIC_WRITE);
            em.merge(device);

    }

    @Transactional
    public void registerDevice(Device device) {
        em.persist(device);
    }

    @Transactional
    public List<Device> findByUUIDListAndUser(User user, List<UUID> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDListAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    @Transactional
    public Device findByUUIDAndUser(User user,UUID guid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guid", guid);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }


    @Transactional
    public List<Device> findByUUID(List<UUID> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByListUUID", Device.class);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    @Transactional
    public List<Device> findByUUIDAndUserAndTimestamp(User user, List<UUID> list, Date timestamp) {
        try{
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndUserAndTimestamp", Device.class);
        query.setParameter("user", user);
        query.setParameter("guidList", list);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
        }
        catch (Exception e){
            e.getMessage();
            return null;
        }
    }


}
