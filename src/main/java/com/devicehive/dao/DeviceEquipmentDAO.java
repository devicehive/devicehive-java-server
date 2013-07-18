package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Stateless
@Interceptors(ValidationInterceptor.class)
public class DeviceEquipmentDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;


    public void saveDeviceEquipment(DeviceEquipment deviceEquipment) {
        em.persist(deviceEquipment);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceEquipment findById(Long id) {
        return em.find(DeviceEquipment.class, id);
    }

    public int update(DeviceEquipment deviceEquipment){
        Query query = em.createNamedQuery("DeviceEquipment.updateByCodeAndDevice");
        query.setParameter("timestamp", new Date());
        query.setParameter("parameters", deviceEquipment.getParameters());
        query.setParameter("device", deviceEquipment.getDevice());
        query.setParameter("code", deviceEquipment.getCode());
        return query.executeUpdate();
    }

}
