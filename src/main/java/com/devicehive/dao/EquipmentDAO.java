package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

@Stateless
public class EquipmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public void saveEquipment(Equipment equipment){
        em.persist(equipment);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
