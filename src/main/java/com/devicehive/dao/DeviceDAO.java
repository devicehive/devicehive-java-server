package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Stateless
@EJB(beanInterface = DeviceDAO.class, name = "DeviceDAO")
public class DeviceDAO {

    private static Logger logger = LoggerFactory.getLogger(DeviceDAO.class);
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findById(Long id) {
        return em.find(Device.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUID(UUID uuid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUID", Device.class);
        query.setParameter("uuid", uuid);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndKey(UUID uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndKey", Device.class);
        query.setParameter("uuid", uuid);
        query.setParameter("key", key);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUIDListAndUser(User user, List<UUID> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDListAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndUser(User user, UUID guid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guid", guid);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUID(List<UUID> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByListUUID", Device.class);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    public Device createDevice(Device device) {
        em.persist(device);
        return device;
    }

    public boolean updateDevice(@NotNull Long id, Device device) {
        Query query = em.createNamedQuery("Device.updateById");
        query.setParameter("name", device.getName());
        query.setParameter("status", device.getStatus());
        query.setParameter("network", device.getNetwork());
        query.setParameter("deviceClass", device.getClass());
        query.setParameter("data", device.getData());
        query.setParameter("id", device.getId());
        return query.executeUpdate() != 0;
    }

    public boolean deleteDevice(@NotNull Long id) {
        Query query = em.createNamedQuery("Device.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public int deleteDeviceByFK(@NotNull Network network) {
        Query query = em.createNamedQuery("Device.deleteByNetwork");
        query.setParameter("network", network);
        return query.executeUpdate();
    }


}
