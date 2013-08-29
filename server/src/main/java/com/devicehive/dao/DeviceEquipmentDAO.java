package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;

@Stateless
public class DeviceEquipmentDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public DeviceEquipment createDeviceEquipment(DeviceEquipment deviceEquipment) {
        em.persist(deviceEquipment);
        return deviceEquipment;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceEquipment findById(Long id) {
        return em.find(DeviceEquipment.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceEquipment findByCodeAndDevice(String code, Device device) {
        TypedQuery<DeviceEquipment> query = em.createNamedQuery("DeviceEquipment.getByDeviceAndCode",
                DeviceEquipment.class);
        query.setParameter("code", code);
        query.setParameter("device", device);
        CacheHelper.cacheable(query);
        List<DeviceEquipment> queryResult = query.getResultList();
        return queryResult.isEmpty() ? null : queryResult.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceEquipment> findByFK(@NotNull Device device) {
        TypedQuery<DeviceEquipment> query = em.createNamedQuery("DeviceEquipment.getByDevice",
                DeviceEquipment.class);
        query.setParameter("device", device);
        return query.getResultList();
    }

    public boolean update(DeviceEquipment deviceEquipment) {
        DeviceEquipment equipment = findByCodeAndDevice(deviceEquipment.getCode(), deviceEquipment.getDevice());
        if (equipment == null) {
            return false;
        }
        equipment.setTimestamp(deviceEquipment.getTimestamp());
        equipment.setParameters(deviceEquipment.getParameters());
        return true;
    }

    public boolean deleteDeviceEquipment(@NotNull Long id) {
        Query query = em.createNamedQuery("DeviceEquipment.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public int deleteByFK(@NotNull Device device) {
        Query query = em.createNamedQuery("DeviceEquipment.deleteByFK");
        query.setParameter("device", device);
        return query.executeUpdate();
    }


}
