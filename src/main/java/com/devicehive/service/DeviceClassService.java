package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceClassDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.*;

@Component
public class DeviceClassService {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Autowired
    private DeviceClassDAO deviceClassDAO;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private HiveValidator hiveValidator;

    public boolean delete(@NotNull long id) {
        return deviceClassDAO.delete(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceClass getWithEquipment(@NotNull long id) {
        return deviceClassDAO.getWithEquipment(id);
    }


    @Transactional
    public DeviceClass createOrUpdateDeviceClass(NullableWrapper<DeviceClassUpdate> deviceClass,
                                                 Set<Equipment> customEquipmentSet) {
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
            if (Boolean.FALSE.equals(stored.getPermanent())) {
                em.refresh(stored, LockModeType.PESSIMISTIC_WRITE);
                if (deviceClass.getValue().getData() != null) {
                    stored.setData(deviceClassFromMessage.getData());
                }
                if (deviceClass.getValue().getOfflineTimeout() != null) {
                    stored.setOfflineTimeout(deviceClassFromMessage.getOfflineTimeout());
                }
                if (deviceClass.getValue().getPermanent() != null) {
                    stored.setPermanent(deviceClassFromMessage.getPermanent());
                }
                Set<Equipment> eq = deviceClassFromMessage.getEquipment();
                eq = eq != null ? eq : customEquipmentSet;
                if (eq != null) {
                    replaceEquipment(eq, stored);
                }
            }
            return stored;
        } else {
            //create
            if (deviceClassFromMessage.getId() != null) {
                throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
            }
            deviceClassDAO.createDeviceClass(deviceClassFromMessage);
            Set<Equipment> eq = deviceClassFromMessage.getEquipment();
            eq = eq != null ? eq : customEquipmentSet;
            if (eq != null) {
                replaceEquipment(eq, deviceClassFromMessage);
            }
            return deviceClassFromMessage;
        }
    }

    @Transactional
    public DeviceClass addDeviceClass(DeviceClass deviceClass) {
        if (deviceClass.getId() != null) {
            throw new HiveException(Messages.ID_NOT_ALLOWED, BAD_REQUEST.getStatusCode());
        }
        if (deviceClassDAO.getDeviceClassByNameAndVersion(deviceClass.getName(), deviceClass.getVersion()) != null) {
            throw new HiveException(Messages.DEVICE_CLASS_WITH_SUCH_NAME_AND_VERSION_EXISTS, FORBIDDEN.getStatusCode());
        }
        DeviceClass createdDeviceClass = deviceClassDAO.createDeviceClass(deviceClass);
        if (deviceClass.getEquipment() != null) {
            Set<Equipment> resultEquipment = createEquipment(createdDeviceClass, deviceClass.getEquipment());
            createdDeviceClass.setEquipment(resultEquipment);
        }
        return createdDeviceClass;
    }

    @Transactional
    public void update(@NotNull Long id, DeviceClassUpdate update) {
        DeviceClass stored = deviceClassDAO.getDeviceClass(id);
        if (stored == null) {
            throw new HiveException(String.format(Messages.DEVICE_CLASS_NOT_FOUND, id),
                                    Response.Status.NOT_FOUND.getStatusCode());
        }
        if (update == null) {
            return;
        }
        if (update.getData() != null) {
            stored.setData(update.getData().getValue());
        }
        if (update.getEquipment() != null) {
            replaceEquipment(update.getEquipment().getValue(), stored);
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
        hiveValidator.validate(stored);
        deviceClassDAO.updateDeviceClass(stored);
    }

    @Transactional
    public void replaceEquipment(@NotNull Collection<Equipment> equipmentsToReplace,
                                 @NotNull DeviceClass deviceClass) {
        equipmentService.deleteByDeviceClass(deviceClass);
        Set<String> codes = new HashSet<>(equipmentsToReplace.size());
        for (Equipment newEquipment : equipmentsToReplace) {
            if (codes.contains(newEquipment.getCode())) {
                throw new HiveException(
                    String.format(Messages.DUPLICATE_EQUIPMENT_ENTRY, newEquipment.getCode(), deviceClass.getId()),
                    FORBIDDEN.getStatusCode());
            }
            codes.add(newEquipment.getCode());
            newEquipment.setDeviceClass(deviceClass);
            equipmentService.create(newEquipment);
        }
    }

    @Transactional
    public Set<Equipment> createEquipment(@NotNull DeviceClass deviceClass, @NotNull Set<Equipment> equipments) {
        Set<String> existingCodesSet = new HashSet<>(equipments.size());

        for (Equipment equipment : equipments) {
            if (existingCodesSet.contains(equipment.getCode())) {
                throw new HiveException(
                    String.format(Messages.DUPLICATE_EQUIPMENT_ENTRY, equipment.getCode(), deviceClass.getId()),
                    FORBIDDEN.getStatusCode());
            }
            existingCodesSet.add(equipment.getCode());
            equipment.setDeviceClass(deviceClass);
            equipmentService.create(equipment);
        }
        return equipments;
    }

    @Transactional
    public Equipment createEquipment(Long classId, Equipment equipment) {
        DeviceClass deviceClass = deviceClassDAO.getDeviceClass(classId);

        if (deviceClass == null) {
            throw new HiveException(String.format(Messages.DEVICE_CLASS_NOT_FOUND, classId), NOT_FOUND.getStatusCode());
        }
        if (deviceClass.getPermanent()) {
            throw new HiveException(Messages.UPDATE_PERMANENT_EQUIPMENT, NOT_FOUND.getStatusCode());
        }
        List<Equipment> equipments = equipmentService.getByDeviceClass(deviceClass);
        String newCode = equipment.getCode();
        if (equipments != null) {
            for (Equipment e : equipments) {
                if (newCode.equals(e.getCode())) {
                    throw new HiveException(
                        String.format(Messages.DUPLICATE_EQUIPMENT_ENTRY, e.getCode(), classId),
                        FORBIDDEN.getStatusCode());
                }
            }
        }
        equipment.setDeviceClass(deviceClass);
        return equipmentService.create(equipment);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {
        return deviceClassDAO.getDeviceClassList(name, namePattern, version, sortField, sortOrderAsc, take, skip);
    }

}
