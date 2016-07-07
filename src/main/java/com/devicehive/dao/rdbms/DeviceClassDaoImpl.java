package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.DeviceClass;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by Gleb on 07.07.2016.
 */
@Repository
public class DeviceClassDaoImpl extends GenericDaoImpl implements DeviceClassDao {
    @Override
    public DeviceClass findByNameAndVersion(String name, String version) {
        return createNamedQuery(DeviceClass.class, "DeviceClass.findByNameAndVersion", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .setParameter("version", version)
                .getResultList()
                .stream().findFirst().orElse(null);
    }
}
