package com.devicehive.dao.riak;

import com.basho.riak.client.core.RiakCluster;
import com.devicehive.dao.EquipmentDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

@Profile("riak")
@Repository
public class EquipmentDaoImpl implements EquipmentDao {

    @Autowired
    private RiakCluster riakCluster;

    @Override
    public List<Equipment> getByDeviceClass(@NotNull DeviceClass deviceClass) {
        return null;
    }

    @Override
    public Equipment getByDeviceClassAndId(@NotNull long deviceClassId, @NotNull long equipmentId) {
        return null;
    }

    @Override
    public int deleteByDeviceClass(@NotNull DeviceClass deviceClass) {
        return 0;
    }

    @Override
    public boolean deleteByIdAndDeviceClass(@NotNull long equipmentId, @NotNull long deviceClassId) {
        return false;
    }

    @Override
    public void persist(Equipment equipment) {

    }

    @Override
    public Equipment find(long equipmentId) {
        return null;
    }

    @Override
    public Equipment merge(Equipment equipment) {
        return null;
    }
}
