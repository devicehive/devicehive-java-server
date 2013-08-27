package com.devicehive.service;

import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.dao.EquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.updates.DeviceClassUpdate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.*;

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

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass get(@NotNull long id) {
        return deviceClassDAO.get(id);
    }

    public boolean delete(@NotNull long id) {
        return deviceClassDAO.delete(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getWithEquipment(@NotNull long id) {
        return deviceClassDAO.getWithEquipment(id);
    }

    public DeviceClass createOrUpdateDeviceClass(NullableWrapper<DeviceClassUpdate> deviceClass,
                                                 Set<Equipment> newEquipmentSet,boolean useExistingEquipment) {
        DeviceClass stored;
        //use existing
        if (deviceClass == null) {
            return null;
        }
        //check is already done
        DeviceClass deviceClassFromMessage = deviceClass.getValue().convertTo();
        if (deviceClassFromMessage.getId() != null) {
            stored = deviceClassDAO.getDeviceClass(deviceClassFromMessage.getId());
        } else {
            stored = deviceClassDAO.getDeviceClassByNameAndVersion(deviceClassFromMessage.getName(),
                    deviceClassFromMessage.getVersion());
        }
        if (stored != null) {
            //update
            if (!stored.getPermanent()) {
                if (deviceClass.getValue().getData() != null) {
                    stored.setData(deviceClassFromMessage.getData());
                }
                if (deviceClass.getValue().getOfflineTimeout() != null) {
                    stored.setOfflineTimeout(deviceClassFromMessage.getOfflineTimeout());
                }
                if (deviceClass.getValue().getPermanent() != null) {
                    stored.setPermanent(deviceClassFromMessage.getPermanent());
                }
                if (!useExistingEquipment) {
                    updateEquipment(newEquipmentSet, stored);
                }
            }
            return stored;
        } else {
            //create
            if (deviceClassFromMessage.getId() != null) {
                throw new HiveException("Invalid request");
            }
            deviceClassDAO.createDeviceClass(deviceClassFromMessage);
            if (!useExistingEquipment) {
                updateEquipment(newEquipmentSet, deviceClassFromMessage);
            }
            return deviceClassFromMessage;
        }
    }

    public DeviceClass addDeviceClass(DeviceClass deviceClass) {
        if (deviceClass.getId() != null) {
            throw new HiveException("Invalid request. Id cannot be specified.", BAD_REQUEST.getStatusCode());
        }
        if (deviceClassDAO.getDeviceClassByNameAndVersion(deviceClass.getName(), deviceClass.getVersion()) != null) {
            throw new HiveException("DeviceClass cannot be created. Device with such name and version already " +
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
            throw new HiveException("device with id : " + id + " does not exists");
        }
        if (update.getData() != null)
            stored.setData(update.getData().getValue());
        if (update.getEquipment() != null) {
            updateEquipment(update.getEquipment().getValue(), stored);
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

    public void updateEquipment(Set<Equipment> newEquipmentSet, DeviceClass deviceClass) {
        List<Equipment> existingEquipments = equipmentDAO.getByDeviceClass(deviceClass);
        if (!newEquipmentSet.isEmpty() && !existingEquipments.isEmpty()) {
            equipmentDAO.delete(existingEquipments);
        }
        for (Equipment equipment : newEquipmentSet) {
            equipment.setDeviceClass(deviceClass);
            equipmentDAO.create(equipment);
        }
    }

    public Equipment createEquipment(Long classId, Equipment equipment) {
        DeviceClass deviceClass = deviceClassDAO.get(classId);

        if (deviceClass == null) {
            throw new HiveException("No device class with id = " + classId + " found", NOT_FOUND.getStatusCode());
        }
        if (deviceClass.getPermanent()) {
            throw new HiveException("Unable to update equipment on permanent device class.",
                    NOT_FOUND.getStatusCode());
        }
        List<Equipment> equipments = equipmentDAO.getByDeviceClass(deviceClass);
        String newCode = equipment.getCode();
        if (equipments != null) {
            for (Equipment e : equipments) {
                if (newCode.equals(e.getCode())) {
                    throw new HiveException("Equipment with code = " + newCode + " and device class id = " + classId +
                            " already exists");
                }
            }
        }
        equipment.setDeviceClass(deviceClass);
        return equipmentDAO.create(equipment);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {
        return deviceClassDAO.getDeviceClassList(name, namePattern, version, sortField, sortOrderAsc, take, skip);
    }

}
