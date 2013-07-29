package com.devicehive.service;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.EquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.dao.DublicateEntryException;
import com.devicehive.exceptions.dao.HivePersistingException;
import com.devicehive.exceptions.dao.NoSuchRecordException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.DeviceClassUpdate;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
@Stateless
public class DeviceClassService {

    @Inject
    private DeviceClassDAO deviceClassDAO;
    @Inject
    private EquipmentDAO equipmentDAO;
    @Inject
    private DeviceService deviceService;

    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField,
                                                String sortOrder, Integer take, Integer skip) {
        return deviceClassDAO.getDeviceClassList(name, namePattern, version, sortField, "ASC".equals(sortOrder), take, skip);
    }

    public DeviceClass get(@NotNull long id) {
        return deviceClassDAO.get(id);
    }

    public boolean delete(@NotNull long id) {
        return deviceClassDAO.delete(id);
    }

    public DeviceClass getWithEquipment(@NotNull long id) {
        return deviceClassDAO.getWithEquipment(id);
    }

    public DeviceClass addDeviceClass(DeviceClass deviceClass) throws DublicateEntryException {

        if (deviceClass.getPermanent() == null) {
            throw new HivePersistingException("Unable to persisst DeviceClass without 'permanent' property");
        }

        if (deviceClassDAO.getDeviceClassByNameAndVersion(deviceClass.getName(), deviceClass.getVersion()) != null) {
            throw new DublicateEntryException("Device with such name and version already exists");
        }
        return deviceClassDAO.createDeviceClass(deviceClass);
    }

    public DeviceClass update(DeviceClass deviceClass) {
        if (deviceClass.getName() != null && deviceClass.getVersion() != null) {
            DeviceClass existingDeviceClass = deviceClassDAO.getDeviceClassByNameAndVersion(deviceClass.getName(), deviceClass.getVersion());
            if (existingDeviceClass != null && !deviceClass.equals(existingDeviceClass)) {
                throw new DublicateEntryException("Entity with same name and version already exists with different id");
            }
        }
        try {
            DeviceClass recordToUpdate = deviceClassDAO.get(deviceClass.getId());
            if (deviceClass.getName() != null) {
                recordToUpdate.setName(deviceClass.getName());
            }
            if (deviceClass.getVersion() != null) {
                recordToUpdate.setVersion(deviceClass.getVersion());
            }
            if (deviceClass.getPermanent() != null) {
                recordToUpdate.setPermanent(deviceClass.getPermanent());
            }
            if (deviceClass.getOfflineTimeout() != null) {
                recordToUpdate.setOfflineTimeout(deviceClass.getOfflineTimeout());
            }
            if (deviceClass.getData() != null) {
                recordToUpdate.setData(deviceClass.getData());
            }

            return deviceClassDAO.updateDeviceClass(recordToUpdate);
        } catch (Exception e) {
            throw new HivePersistingException("Persisting Exception", e);
        }
    }

    public void update(long id, DeviceClassUpdate update) {
        if (update == null) {
            return;
        }
        DeviceClass stored = deviceClassDAO.getDeviceClass(id);
        if (stored == null) {
            throw new HiveException("device with id = " + id + " does not exists");
        }
        if (update.getData() != null)
            stored.setData(update.getData().getValue());
        if (update.getEquipment() != null) {
            deviceService.updateEquipment(update.getEquipment().getValue(), stored);
            stored.setEquipment(update.getEquipment().getValue());
        }
        if (update.getName() != null) {
            stored.setName(update.getName().getValue());
        }
        if (update.getPermanent() != null) {
            stored.setPermanent(update.getPermanent().getValue());
        }
        if (update.getOfflineTimeout() != null) {
            stored.setOfflineTimeout(update.getOfflineTimeout().getValue());
        }
        if (update.getVersion() != null) {
            stored.setVersion(update.getVersion().getValue());
        }
        deviceClassDAO.updateDeviceClass(stored);

    }

}
