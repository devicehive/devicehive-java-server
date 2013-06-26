package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public class EquipmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassDAO.class);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public Equipment findByCode(String code){
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.findByCode", Equipment.class);
        query.setParameter("code", code);
        List<Equipment> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional
    public void saveEquipment(Equipment ... equipment){
        for (Equipment e: equipment){
            em.persist(e);
        }
        em.flush();
    }

    @Transactional
    public void updateEquipment(Equipment ... equipment){
        for (Equipment e: equipment){
            em.merge(e);
        }
        em.flush();
    }

    @Transactional
    public void saveOrUpdateEquipments(Set<Equipment> equipmentSet){
        for (Equipment equipment: equipmentSet){
            Equipment findByCodeEquipment =  findByCode(equipment.getCode());
            if (findByCodeEquipment == null){
                em.persist(equipment);
            }
            else{
                equipment.setId(findByCodeEquipment.getId());
                em.merge(equipment);
            }
        }
        em.flush();
    }


    @Transactional
    public void removeUnusefulEquipments(DeviceClass deviceClass, Set<Equipment> equipmentSet){
        TypedQuery<Equipment> query = em.createNamedQuery("Equipment.getByDeviceClass", Equipment.class);
        query.setParameter("deviceClass", deviceClass);
        List<Equipment> existingEquipments = query.getResultList();
        for (Equipment existingEquipment: existingEquipments){
            boolean shouldRemove = true;
            for (Equipment newEquipment: equipmentSet){
                if (newEquipment.getCode().equals(existingEquipment.getCode())){
                    shouldRemove = false;
                }
            }
            if (shouldRemove){
                em.remove(existingEquipment);
            }
        }
        em.flush();
    }
}
