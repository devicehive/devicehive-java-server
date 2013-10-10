package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

@Stateless
public class EquipmentDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    /**
     * Inserts new record
     *
     * @param equipment Equipment instance to save
     * @Return managed instance of Equipment
     */
    public Equipment create(@NotNull Equipment equipment) {
        em.persist(equipment);
        return equipment;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Equipment> getByDeviceClass(DeviceClass deviceClass) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClass", Equipment.class);
        query.setParameter("deviceClass", deviceClass);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Equipment> getByDeviceClassAndCode(DeviceClass deviceClass) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClass", Equipment.class);
        query.setParameter("deviceClass", deviceClass);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Equipment getByDeviceClass(@NotNull long deviceClassId, @NotNull long equipmentId) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClassAndId", Equipment.class);
        query.setParameter("equipmentId", equipmentId);
        query.setParameter("deviceClassId", deviceClassId);
        List<Equipment> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }


    public boolean delete(@NotNull long equipmentId) {
        Query query = em.createNamedQuery("Equipment.deleteById");
        query.setParameter("id", equipmentId);
        return query.executeUpdate() != 0;
    }

    public boolean delete(@NotNull long equipmentId, @NotNull long deviceClassId) {
        Query query = em.createNamedQuery("Equipment.deleteByIdAndDeviceClass");
        query.setParameter("id", equipmentId);
        query.setParameter("deviceClassId", deviceClassId);
        return query.executeUpdate() != 0;
    }

    /**
     * @param id Equipment Id
     * @returns Equipment
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Equipment get(@NotNull long id) {
        return em.find(Equipment.class, id);
    }

    /**
     * @param equipments equipments to remove
     * @return
     */
    public int delete(Collection<Equipment> equipments) {
        Query query = em.createNamedQuery("Equipment.deleteByEquipmentList");
        query.setParameter("equipmentList", equipments);
        return query.executeUpdate();
    }

    public int deleteByDeviceClass(DeviceClass deviceClass) {
        Query query = em.createNamedQuery("Equipment.deleteByDeviceClass");
        query.setParameter("deviceClass", deviceClass);
        return query.executeUpdate();
    }
}
