package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.EquipmentDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Profile({"rdbms"})
@Repository
public class EquipmentDaoImpl extends GenericDaoImpl implements EquipmentDao {
    @Override
    public List<Equipment> getByDeviceClass(@NotNull DeviceClass deviceClass) {
        return createNamedQuery(Equipment.class, "Equipment.getByDeviceClass", Optional.of(CacheConfig.get()))
                .setParameter("deviceClass", deviceClass)
                .getResultList();
    }

    @Override
    public Equipment getByDeviceClassAndId(@NotNull String deviceClassName, @NotNull long equipmentId) {
        return createNamedQuery(Equipment.class, "Equipment.getByDeviceClassAndId", Optional.of(CacheConfig.get()))
                .setParameter("id", equipmentId)
                .setParameter("deviceClassName", deviceClassName)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public int deleteByDeviceClass(@NotNull DeviceClass deviceClass) {
        return createNamedQuery("Equipment.deleteByDeviceClass", Optional.<CacheConfig>empty())
                .setParameter("deviceClass", deviceClass)
                .executeUpdate();
    }

    @Override
    public boolean deleteByIdAndDeviceClass(@NotNull long equipmentId, @NotNull String deviceClassName) {
        return createNamedQuery("Equipment.deleteByIdAndDeviceClass", Optional.<CacheConfig>empty())
                .setParameter("id", equipmentId)
                .setParameter("deviceClassName", deviceClassName)
                .executeUpdate() != 0;
    }

    @Override
    public void persist(Equipment equipment) {
        super.persist(equipment);
    }

    @Override
    public Equipment find(long equipmentId) {
        return super.find(Equipment.class, equipmentId);
    }

    @Override
    public Equipment merge(Equipment equipment) {
        return super.merge(equipment);
    }
}
