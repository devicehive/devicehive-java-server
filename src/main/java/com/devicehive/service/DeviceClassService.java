package com.devicehive.service;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.dao.EquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.DeviceClassUpdate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;
import java.util.List;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
@Stateless
public class DeviceClassService {

    @EJB
    private DeviceClassDAO deviceClassDAO;
    @EJB
    private EquipmentDAO equipmentDAO;
    @EJB
    private DeviceService deviceService;

    public DeviceClass get(@NotNull long id) {
        return deviceClassDAO.get(id);
    }

    public boolean delete(@NotNull long id) {
        return deviceClassDAO.delete(id);
    }

    public DeviceClass getWithEquipment(@NotNull long id) {
        return deviceClassDAO.getWithEquipment(id);
    }

    public DeviceClass addDeviceClass(DeviceClass deviceClass){

        if (deviceClassDAO.getDeviceClassByNameAndVersion(deviceClass.getName(), deviceClass.getVersion()) != null) {
            throw new HiveException("DeviceClass couldn't be created. Device with such name and version already " +
                    "exists", FORBIDDEN.getStatusCode());
        }
        return deviceClassDAO.createDeviceClass(deviceClass);
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

    public Equipment createEquipment(Long classId, Equipment equipment) {
        DeviceClass deviceClass = deviceClassDAO.get(classId);

        if (deviceClass == null) {
            throw new NoResultException("No device class with id = " + classId + "found");
        }

        List<Equipment> equipments = equipmentDAO.getByDeviceClass(deviceClass);
        String newCode = equipment.getCode();
        for (Equipment e : equipments) {
            if (newCode.equals(e.getCode())) {
                throw new HiveException("Equipment with code = " + newCode + " and device class id = " + classId +
                        " already exists");
            }
        }
        equipment.setDeviceClass(deviceClass);
        return equipmentDAO.create(equipment);
    }

    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {
        return deviceClassDAO.getDeviceClassList(name, namePattern, version, sortField, sortOrderAsc, take, skip);
    }

}
