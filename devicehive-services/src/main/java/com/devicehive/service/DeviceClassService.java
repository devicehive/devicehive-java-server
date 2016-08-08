package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.EquipmentUpdate;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class DeviceClassService {

    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private DeviceClassDao deviceClassDao;

    @Transactional
    public void delete(@NotNull long id) {
        deviceClassDao.remove(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceClassWithEquipmentVO getWithEquipment(@NotNull Long id) {
        return deviceClassDao.find(id);
    }

    @Transactional
    public DeviceClassWithEquipmentVO createOrUpdateDeviceClass(Optional<DeviceClassUpdate> deviceClass, Set<DeviceClassEquipmentVO> customEquipmentSet) {
        DeviceClassWithEquipmentVO stored;
        //use existing
        if (deviceClass == null || !deviceClass.isPresent()) {
            return null;
        }
        //check is already done
        DeviceClassUpdate deviceClassUpdate = deviceClass.orElse(null);
        DeviceClassWithEquipmentVO deviceClassFromMessage = deviceClassUpdate.convertTo();
        if (deviceClassFromMessage.getId() != null) {
            stored = deviceClassDao.find(deviceClassFromMessage.getId());
        } else {
            stored = deviceClassDao.findByName(deviceClassFromMessage.getName());
        }
        if (stored != null) {
            if (!stored.getIsPermanent()) {
                deviceClassUpdate.setEquipment(Optional.ofNullable(customEquipmentSet));
                update(stored.getId(), deviceClassUpdate);
            }
            return stored;
        } else {
            return addDeviceClass(deviceClassFromMessage);
        }
    }

    @Transactional
    public DeviceClassWithEquipmentVO addDeviceClass(DeviceClassWithEquipmentVO deviceClass) {
        if (deviceClassDao.findByName(deviceClass.getName()) != null) {
            throw new HiveException(Messages.DEVICE_CLASS_WITH_SUCH_NAME_AND_VERSION_EXISTS, FORBIDDEN.getStatusCode());
        }
        if (deviceClass.getIsPermanent() == null) {
            deviceClass.setIsPermanent(false);
        }
        hiveValidator.validate(deviceClass);
        deviceClass = deviceClassDao.persist(deviceClass);
        // TODO [rafa] looks strange, very strange
//        if (deviceClass.getEquipment() != null) {
//            Set<Equipment> resultEquipment = createEquipment(deviceClass, deviceClass.getEquipment());
//            deviceClass.setEquipment(resultEquipment);
//        }
        return deviceClass;
    }

    @Transactional
    public DeviceClassWithEquipmentVO update(@NotNull Long id, DeviceClassUpdate update) {
        DeviceClassWithEquipmentVO stored = deviceClassDao.find(id);
        if (stored == null) {
            throw new HiveException(String.format(Messages.DEVICE_CLASS_NOT_FOUND, id), Response.Status.NOT_FOUND.getStatusCode());
        }
        if (update == null) {
            return null;
        }
        if (update.getData() != null) {
            stored.setData(update.getData().orElse(null));
        }
        if (update.getEquipment() != null) {
            if (update.getEquipment().isPresent()) {
                Map<String, DeviceClassEquipmentVO> existing = new HashMap<>();
                for (DeviceClassEquipmentVO deviceClassEquipmentVO : stored.getEquipment()) {
                    existing.put(deviceClassEquipmentVO.getCode(), deviceClassEquipmentVO);
                }

                for (DeviceClassEquipmentVO deviceClassEquipmentVO : update.getEquipment().get()) {
                    if (existing.containsKey(deviceClassEquipmentVO.getCode())) {
                        Long existingEquipmentId = existing.get(deviceClassEquipmentVO.getCode()).getId();
                        deviceClassEquipmentVO.setId(existingEquipmentId);
                    }
                }
                stored.setEquipment(update.getEquipment().orElse(null));
            }
        }
        if (update.getId() != null) {
            stored.setId(update.getId());
        }
        if (update.getName() != null) {
            stored.setName(update.getName().orElse(null));
        }
        if (update.getPermanent() != null) {
            stored.setIsPermanent(update.getPermanent().orElse(null));
        }
        if (update.getOfflineTimeout() != null) {
            stored.setOfflineTimeout(update.getOfflineTimeout().orElse(null));
        }
        hiveValidator.validate(stored);
        return deviceClassDao.merge(stored);
    }

    /**
     * updates Equipment attributes
     *
     * @param equipmentUpdate Equipment instance, containing fields to update (Id field will be ignored)
     * @param equipmentId     id of equipment to update
     * @param deviceClassId   class of equipment to update
     * @return true, if update successful, false otherwise
     */
    @Transactional
    public boolean update(EquipmentUpdate equipmentUpdate, @NotNull long equipmentId, @NotNull long deviceClassId) {
        if (equipmentUpdate == null) {
            return true;
        }
        DeviceClassWithEquipmentVO stored = deviceClassDao.find(deviceClassId);
        DeviceClassEquipmentVO found = null;
        for (DeviceClassEquipmentVO deviceClassEquipmentVO : stored.getEquipment()) {
            if (deviceClassEquipmentVO.getId().equals(equipmentId)) {
                found = deviceClassEquipmentVO;
            }
        }

        if (found == null) {
            return false; // equipment with id = equipmentId does not exists
        }
        if (equipmentUpdate.getCode() != null) {
            found.setCode(equipmentUpdate.getCode().orElse(null));
        }
        if (equipmentUpdate.getName() != null) {
            found.setName(equipmentUpdate.getName().orElse(null));
        }
        if (equipmentUpdate.getType() != null) {
            found.setType(equipmentUpdate.getType().orElse(null));
        }
        if (equipmentUpdate.getData() != null) {
            found.setData(equipmentUpdate.getData().orElse(null));
        }
        deviceClassDao.merge(stored);
        return true;
    }

    @Transactional
    public Set<DeviceClassEquipmentVO> createEquipment(@NotNull Long classId, @NotNull Set<DeviceClassEquipmentVO> equipments) {
        DeviceClassWithEquipmentVO deviceClass = deviceClassDao.find(classId);
        Set<String> existingCodesSet = deviceClass.getEquipment().stream().map(DeviceClassEquipmentVO::getCode).collect(Collectors.toSet());
        Set<String> newCodeSet = equipments.stream().map(DeviceClassEquipmentVO::getCode).collect(Collectors.toSet());

        newCodeSet.retainAll(existingCodesSet);
        if (!newCodeSet.isEmpty()) {
            String codeSet = StringUtils.join(newCodeSet, ",");
            throw new HiveException(String.format(Messages.DUPLICATE_EQUIPMENT_ENTRY, codeSet, classId), FORBIDDEN.getStatusCode());
        }

        deviceClass.getEquipment().addAll(equipments);

        deviceClass = deviceClassDao.merge(deviceClass);

        //TODO [rafa] duuumb, and lazy, in case of several equipments linked to the class two for loops is fine.
        for (DeviceClassEquipmentVO equipment : equipments) {
            for (DeviceClassEquipmentVO s : deviceClass.getEquipment()) {
                if (equipment.getCode().equals(s.getCode())) {
                    equipment.setId(s.getId());
                }
            }
        }

        return equipments;
    }

    @Transactional
    public DeviceClassEquipmentVO createEquipment(Long classId, DeviceClassEquipmentVO equipment) {
        DeviceClassWithEquipmentVO deviceClass = deviceClassDao.find(classId);
        if (deviceClass == null) {
            throw new HiveException(String.format(Messages.DEVICE_CLASS_NOT_FOUND, classId), NOT_FOUND.getStatusCode());
        }
        if (deviceClass.getIsPermanent()) {
            throw new HiveException(Messages.UPDATE_PERMANENT_EQUIPMENT, NOT_FOUND.getStatusCode());
        }
        Set<DeviceClassEquipmentVO> equipments = deviceClass.getEquipment();
        String newCode = equipment.getCode();
        if (equipments != null) {
            for (DeviceClassEquipmentVO e : equipments) {
                if (newCode.equals(e.getCode())) {
                    String errorMessage = String.format(Messages.DUPLICATE_EQUIPMENT_ENTRY, e.getCode(), classId);
                    throw new HiveException(errorMessage, FORBIDDEN.getStatusCode());
                }
            }
        }

        deviceClass.getEquipment().add(equipment);
        deviceClass = deviceClassDao.merge(deviceClass);

        //TODO [rafa] find device equipment class back from the set in device class.
        for (DeviceClassEquipmentVO s : deviceClass.getEquipment()) {
            if (equipment.getCode().equals(s.getCode())) {
                equipment.setId(s.getId());
            }
        }

        return equipment;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<DeviceClassWithEquipmentVO> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {
        return deviceClassDao.getDeviceClassList(name, namePattern, sortField, sortOrderAsc, take, skip);
    }

    /**
     * Delete Equipment (not DeviceEquipment, but whole equipment with appropriate device Equipments)
     *
     * @param equipmentId   equipment id to delete
     * @param deviceClassId id of deviceClass which equipment belongs used to double check
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(@NotNull long equipmentId, @NotNull long deviceClassId) {
        DeviceClassWithEquipmentVO stored = deviceClassDao.find(deviceClassId);
        DeviceClassEquipmentVO found = null;
        Iterator<DeviceClassEquipmentVO> iterator = stored.getEquipment().iterator();
        while (iterator.hasNext()) {
            DeviceClassEquipmentVO deviceClassEquipmentVO = iterator.next();
            if (deviceClassEquipmentVO.getId().equals(equipmentId)) {
                iterator.remove();
                found = deviceClassEquipmentVO;
            }
        }

        if (found != null) {
            deviceClassDao.merge(stored);
        }

        return found != null;
    }

    /**
     * Retrieves Equipment from database
     *
     * @param deviceClassId parent device class id for this equipment
     * @param equipmentId   id of equipment to get
     */
    @Transactional(readOnly = true)
    public DeviceClassEquipmentVO getByDeviceClass(@NotNull long deviceClassId, @NotNull long equipmentId) {
        return deviceClassDao.findDeviceClassEquipment(deviceClassId, equipmentId);
    }

    /**
     * Retrieves Equipment from database
     *
     * @param deviceClassId parent device class id for this equipment
     */
    @Transactional(readOnly = true)
    public Set<DeviceClassEquipmentVO> getByDeviceClass(@NotNull long deviceClassId) {
        return deviceClassDao.find(deviceClassId).getEquipment();
    }

}
