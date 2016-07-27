package com.devicehive.service;

import com.devicehive.dao.EquipmentDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.EquipmentUpdate;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * This class manages equipment in database. EquipmentDAO shouldn't be used directly from controller, please use this
 * class instead
 */
@Deprecated
@Component
@Transactional(propagation = Propagation.SUPPORTS)
public class EquipmentService {

    @Autowired
    private HiveValidator validationUtil;
    @Autowired
    private EquipmentDao equipmentDao;

}
