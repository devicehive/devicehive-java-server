package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.dao.HivePersistingException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.*;
import java.util.Collection;
import java.util.List;

@Stateless
public class EquipmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public Equipment createEquipment(Equipment equipment) {
        em.persist(equipment);
        return equipment;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Equipment> getByDeviceClass(DeviceClass deviceClass) {
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClass", Equipment.class);
        query.setParameter("deviceClass", deviceClass);
        return query.getResultList();
    }

    @Deprecated
    public Equipment getByDeviceClass(long deviceClassId, long equipmentId) {

        Equipment e = em.find(Equipment.class, equipmentId);

        if (e == null || e.getDeviceClass().getId() != deviceClassId) {
            return null;
        }

        return e;

    }

    /**
     * Inserts new record
     *
     * @param e Equipment instance to save
     * @Return managed instance of Equipment
     */
    @Deprecated
    public Equipment insert(Equipment e) throws PersistenceException {
        try {
            return em.merge(e);
        } catch (Exception ex) {
            throw new HivePersistingException("Unable to persist entity", ex);
        }
    }

    @Deprecated
    public Equipment update(Equipment e) throws PersistenceException {
        try {
            return em.merge(e);
        } catch (Exception ex) {
            throw new HivePersistingException("Unable to persist entity", ex);
        }
    }

    //TODO api
    @Deprecated
    public void delete(Equipment e) {
        em.remove(e);
    }

    /**
     * returns Equipment by id
     */
    public Equipment get(long id) {
        return em.find(Equipment.class, id);
    }

    /**
     * @param equipments equipments to remove
     * @return
     */
    public int removeEquipment(Collection<Equipment> equipments) {
        Query query = em.createNamedQuery("Equipment.deleteByEquipmentList");
        query.setParameter("equipmentList", equipments);
        return query.executeUpdate();
    }
}
