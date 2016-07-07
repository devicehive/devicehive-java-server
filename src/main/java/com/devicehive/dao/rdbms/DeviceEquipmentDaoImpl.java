package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

import static java.util.Optional.of;

/**
 * Created by Gleb on 07.07.2016.
 */
@Repository
public class DeviceEquipmentDaoImpl extends GenericDaoImpl implements DeviceEquipmentDao {
    @Override
    public List<DeviceEquipment> getByDevice(Device device) {
        return createNamedQuery(DeviceEquipment.class, DeviceEquipment.Queries.Names.GET_BY_DEVICE,
                of(CacheConfig.refresh()))
                .setParameter("device", device)
                .getResultList();
    }

    @Override
    public DeviceEquipment getByDeviceAndCode(@NotNull String code, @NotNull Device device) {
        return createNamedQuery(DeviceEquipment.class, DeviceEquipment.Queries.Names.GET_BY_DEVICE_AND_CODE,
                of(CacheConfig.refresh()))
                .setParameter("code", code)
                .setParameter("device", device)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public DeviceEquipment merge(DeviceEquipment deviceEquipment) {
        return super.merge(deviceEquipment);
    }

    @Override
    public void persist(DeviceEquipment deviceEquipment) {
        super.persist(deviceEquipment);
    }
}
