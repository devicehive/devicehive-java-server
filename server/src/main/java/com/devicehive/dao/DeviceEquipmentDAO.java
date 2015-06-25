package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.devicehive.model.DeviceEquipment.Queries.Names.*;
import static com.devicehive.model.DeviceEquipment.Queries.Parameters.*;

@Component
public class DeviceEquipmentDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public DeviceEquipment createDeviceEquipment(DeviceEquipment deviceEquipment) {
        em.persist(deviceEquipment);
        return deviceEquipment;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceEquipment findById(Long id) {
        return em.find(DeviceEquipment.class, id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceEquipment findByCodeAndDevice(String code, Device device) {
        TypedQuery<DeviceEquipment> query = em.createNamedQuery(GET_BY_DEVICE_AND_CODE, DeviceEquipment.class);
        query.setParameter(CODE, code);
        query.setParameter(DEVICE, device);
        CacheHelper.cacheable(query);
        List<DeviceEquipment> queryResult = query.getResultList();
        return queryResult.isEmpty() ? null : queryResult.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceEquipment> findByFK(@NotNull Device device) {
        TypedQuery<DeviceEquipment> query = em.createNamedQuery(GET_BY_DEVICE, DeviceEquipment.class);
        query.setParameter(DEVICE, device);
        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @Transactional
    public boolean update(DeviceEquipment deviceEquipment) {
        DeviceEquipment equipment = findByCodeAndDevice(deviceEquipment.getCode(), deviceEquipment.getDevice());
        if (equipment == null) {
            return false;
        }
        equipment.setTimestamp(deviceEquipment.getTimestamp());
        equipment.setParameters(deviceEquipment.getParameters());
        return true;
    }

    @Transactional
    public boolean deleteDeviceEquipment(@NotNull Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    @Transactional
    public int deleteByFK(@NotNull Device device) {
        Query query = em.createNamedQuery(DELETE_BY_FK);
        query.setParameter(DEVICE, device);
        return query.executeUpdate();
    }
}
