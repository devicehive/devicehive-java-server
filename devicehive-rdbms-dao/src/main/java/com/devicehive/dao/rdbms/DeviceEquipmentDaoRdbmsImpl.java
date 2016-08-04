package com.devicehive.dao.rdbms;

import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

import static java.util.Optional.of;

/**
 * Created by Gleb on 07.07.2016.
 */
@Profile({"rdbms"})
@Repository
public class DeviceEquipmentDaoRdbmsImpl extends RdbmsGenericDao implements DeviceEquipmentDao {
    @Override
    public List<DeviceEquipmentVO> getByDevice(DeviceVO vo) {
        List<DeviceEquipment> deviceEquipments = createNamedQuery(DeviceEquipment.class, DeviceEquipment.Queries.Names.GET_BY_DEVICE,
                of(CacheConfig.refresh()))
                .setParameter("device", vo.getId())
                .getResultList();
        return DeviceEquipment.convertToVo(deviceEquipments);
    }

    @Override
    public DeviceEquipmentVO getByDeviceAndCode(@NotNull String code, @NotNull DeviceVO device) {
        DeviceEquipment entity = createNamedQuery(DeviceEquipment.class, DeviceEquipment.Queries.Names.GET_BY_DEVICE_AND_CODE,
                of(CacheConfig.refresh()))
                .setParameter("code", code)
                .setParameter("device", device.getId())
                .getResultList()
                .stream().findFirst().orElse(null);
        return DeviceEquipment.convertToVo(entity);
    }

    @Override
    public DeviceEquipmentVO merge(DeviceEquipmentVO deviceEquipment, DeviceVO device) {
        DeviceEquipment entity = DeviceEquipment.convertToEntity(deviceEquipment);
        Device deviceRef = reference(Device.class, device.getId());
        entity.setDevice(deviceRef);
        super.merge(entity);
        return DeviceEquipment.convertToVo(entity);
    }

    @Override
    public void persist(DeviceEquipmentVO deviceEquipment, DeviceVO device) {
        DeviceEquipment entity = DeviceEquipment.convertToEntity(deviceEquipment);
        Device deviceRef = reference(Device.class, device.getId());
        entity.setDevice(deviceRef);
        super.persist(entity);
        deviceEquipment.setId(entity.getId());
    }
}
