package com.devicehive.dao.riak;

import com.devicehive.dao.EquipmentDao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile({"riak"})
@Repository
public class EquipmentDaoRiakImpl implements EquipmentDao {
}
