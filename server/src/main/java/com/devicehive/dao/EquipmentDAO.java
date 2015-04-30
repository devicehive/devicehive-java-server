package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.devicehive.model.Equipment.Queries.Names.*;
import static com.devicehive.model.Equipment.Queries.Parameters.*;

@Stateless
public class EquipmentDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    /**
     * Inserts new record
     *
     * @param equipment Equipment instance to save
     * @return managed instance of Equipment
     */

    public Equipment create(@NotNull Equipment equipment) {
        em.persist(equipment);
        return equipment;
    }

    public Equipment update(@NotNull Equipment equipment) {
        em.merge(equipment);
        return equipment;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Equipment> getByDeviceClass(DeviceClass deviceClass) {
        TypedQuery<Equipment> query = em.createNamedQuery(GET_BY_DEVICE_CLASS, Equipment.class);
        query.setParameter(DEVICE_CLASS, deviceClass);
        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Equipment getByDeviceClass(@NotNull long deviceClassId, @NotNull long equipmentId) {
        TypedQuery<Equipment> query = em.createNamedQuery(GET_BY_DEVICE_CLASS_AND_ID, Equipment.class);
        query.setParameter(ID, equipmentId);
        query.setParameter(DEVICE_CLASS_ID, deviceClassId);
        CacheHelper.cacheable(query);
        List<Equipment> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    public boolean delete(@NotNull long equipmentId, @NotNull long deviceClassId) {
        Query query = em.createNamedQuery(DELETE_BY_ID_AND_DEVICE_CLASS);
        query.setParameter(ID, equipmentId);
        query.setParameter(DEVICE_CLASS_ID, deviceClassId);
        return query.executeUpdate() != 0;
    }

    /**
     * @param id Equipment Id
     * @return Equipment
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Equipment get(@NotNull long id) {
        return em.find(Equipment.class, id);
    }

    public int deleteByDeviceClass(DeviceClass deviceClass) {
        Query query = em.createNamedQuery(DELETE_BY_DEVICE_CLASS);
        query.setParameter(DEVICE_CLASS, deviceClass);
        return query.executeUpdate();
    }
}
