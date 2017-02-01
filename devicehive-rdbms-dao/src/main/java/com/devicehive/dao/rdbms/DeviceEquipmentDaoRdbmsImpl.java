package com.devicehive.dao.rdbms;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

import static java.util.Optional.of;

/**
 * Created by Gleb on 07.07.2016.
 */
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
