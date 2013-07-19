package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.service.interceptors.ValidationInterceptor;
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
import java.util.Collection;
import java.util.List;

@Stateless
@Interceptors(ValidationInterceptor.class)
public class EquipmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Equipment findByCode(String code) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.findByCode", Equipment.class);
        query.setParameter("code", code);
        List<Equipment> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Equipment> getByDeviceClass(DeviceClass deviceClass) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClass", Equipment.class);
        query.setParameter("deviceClass", deviceClass);
        return query.getResultList();
    }

    public Equipment getByDeviceClass(long deviceClassId, long equipmentId) {

        Equipment e = em.find(Equipment.class, equipmentId);

        if (e == null) {
            return null;
        }

        if (e.getDeviceClass().getId() == deviceClassId) {
            return e;
        }

        return null;
    }


    /**
     * Inserts new record
     *
     * @param e Equipment instance to save
     * @Return managed instance of Equipment
     */
    public Equipment insert(Equipment e){
        return em.merge(e);
    }

    public Equipment update(Equipment e){
        return em.merge(e);
    }
    public void delete(Equipment e){
        em.remove(e);
    }

    /**
     * returns Equipment by id
     */
    public Equipment get(long id){
        return em.find(Equipment.class,id);
    }

    /**
     *
     * @param equipments equipments to remove
     * @return
     */
    public int removeEquipment(Collection<Equipment> equipments) {
        Query query = em.createNamedQuery("Equipment.deleteByEquipmentList");
        query.setParameter("equipmentList", equipments);
        return query.executeUpdate();
    }
}
