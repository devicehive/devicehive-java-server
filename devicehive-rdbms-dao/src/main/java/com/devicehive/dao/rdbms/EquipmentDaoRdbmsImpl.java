package com.devicehive.dao.rdbms;

import com.devicehive.dao.EquipmentDao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile({"rdbms"})
@Repository
public class EquipmentDaoRdbmsImpl extends RdbmsGenericDao implements EquipmentDao {
}
