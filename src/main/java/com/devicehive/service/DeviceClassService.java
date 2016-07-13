package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Component
public class DeviceClassService {

    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private DeviceClassDao deviceClassDao;

    @Transactional
    public void delete(@NotNull String name) {
        if (deviceClassDao.find(name) != null) {
            deviceClassDao.remove(deviceClassDao.getReference(name));
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceClass getWithEquipment(@NotNull String id) {
        return deviceClassDao.find(id);
    }

    @Transactional
    public DeviceClass createOrUpdateDeviceClass(Optional<DeviceClassUpdate> deviceClass,
                                                 Set<Equipment> customEquipmentSet) {
        DeviceClass stored;
        //use existing
        if (deviceClass == null) {
            return null;
        }
        //check is already done
        DeviceClass deviceClassFromMessage = deviceClass.orElse(null).convertTo();
        stored = deviceClassDao.find(deviceClassFromMessage.getId());
        if (stored != null) {
            //update
            if (Boolean.FALSE.equals(stored.getPermanent())) {
                deviceClassDao.refresh(stored, LockModeType.PESSIMISTIC_WRITE);
                if (deviceClass.orElse(null).getData() != null) {
                    stored.setData(deviceClassFromMessage.getData());
                }
                if (deviceClass.orElse(null).getOfflineTimeout() != null) {
                    stored.setOfflineTimeout(deviceClassFromMessage.getOfflineTimeout());
                }
                if (deviceClass.orElse(null).getPermanent() != null) {
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
            if (deviceClassFromMessage.getPermanent() == null) {
                deviceClassFromMessage.setPermanent(false);
            }
            deviceClassDao.persist(deviceClassFromMessage);
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
        if (deviceClassDao.find(deviceClass.getId()) != null) {
            throw new HiveException(Messages.DEVICE_CLASS_WITH_SUCH_NAME_AND_VERSION_EXISTS, FORBIDDEN.getStatusCode());
        }
        if (deviceClass.getPermanent() == null) {
            deviceClass.setPermanent(false);
        }
        deviceClassDao.persist(deviceClass);
        if (deviceClass.getEquipment() != null) {
            Set<Equipment> resultEquipment = createEquipment(deviceClass, deviceClass.getEquipment());
            deviceClass.setEquipment(resultEquipment);
        }
        return deviceClass;
    }

    @Transactional
    public void update(@NotNull String id, DeviceClassUpdate update) {
        DeviceClass stored = deviceClassDao.find(id);
        if (stored == null) {
            throw new HiveException(String.format(Messages.DEVICE_CLASS_NOT_FOUND, id),
                                    Response.Status.NOT_FOUND.getStatusCode());
        }
        if (update == null) {
            return;
        }
        if (update.getData() != null) {
            stored.setData(update.getData().orElse(null));
        }
        if (update.getEquipment() != null) {
            replaceEquipment(update.getEquipment().orElse(null), stored);
            stored.setEquipment(update.getEquipment().orElse(null));
        }
        if (update.getId() != null) {
            stored.setId(update.getId().orElse(null));
        }
        if (update.getPermanent() != null) {
            stored.setPermanent(update.getPermanent().orElse(null));
        }
        if (update.getOfflineTimeout() != null) {
            stored.setOfflineTimeout(update.getOfflineTimeout().orElse(null));
        }
        hiveValidator.validate(stored);
        deviceClassDao.merge(stored);
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
    public Equipment createEquipment(String classId, Equipment equipment) {
        DeviceClass deviceClass = deviceClassDao.find(classId);

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
        return deviceClassDao.getDeviceClassList(name, namePattern, sortField, sortOrderAsc, take, skip);
    }

}
