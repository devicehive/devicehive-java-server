package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.Stateless;
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
        deviceEquipment.setTimestamp(new Date());
        em.persist(deviceEquipment);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public DeviceEquipment findById(Long id) {
        return em.find(DeviceEquipment.class, id);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public DeviceEquipment findByCode(String code) {
        TypedQuery<DeviceEquipment> query = em.createNamedQuery("DeviceEquipment.getByCode", DeviceEquipment.class);
        query.setParameter("code", code);
        List<DeviceEquipment> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
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
