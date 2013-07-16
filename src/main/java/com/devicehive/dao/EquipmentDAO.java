package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.service.interceptors.ValidationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

@Stateless
@Interceptors(ValidationInterceptor.class)
public class EquipmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional(Transactional.TxType.SUPPORTS)
    public Equipment findByCode(String code) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.findByCode", Equipment.class);
        query.setParameter("code", code);
        List<Equipment> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Equipment> getByDeviceClass(DeviceClass deviceClass) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClass", Equipment.class);
        query.setParameter("deviceClass", deviceClass);
        return query.getResultList();
    }

    public void removeEquipment(List<Equipment> equipments) {
        Query query = em.createNamedQuery("Equipment.deleteByEquipmentList");
        query.setParameter("equipmentList", equipments);
        query.executeUpdate();
    }
}
