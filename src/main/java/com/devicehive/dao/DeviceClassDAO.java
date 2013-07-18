package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import com.devicehive.service.interceptors.ValidationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;


/**
 * TODO JavaDoc
 */

@Stateless
@Interceptors(ValidationInterceptor.class)
public class DeviceClassDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public List<DeviceClass> getList() {
        return em.createQuery("select dc from DeviceClass dc").getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getDeviceClass(long id) {
        return em.find(DeviceClass.class, id);
    }

    public void saveDeviceClass(DeviceClass deviceClass){
        em.persist(deviceClass);
    }

    public DeviceClass getDeviceClassByNameAndVersion(String name, String version) {
        TypedQuery<DeviceClass> query = em.createNamedQuery("DeviceClass.findByNameAndVersion", DeviceClass.class);
        query.setParameter("version", version);
        query.setParameter("name", name);
        List<DeviceClass> result = query.getResultList();
        return  result.isEmpty() ? null : result.get(0);
    }

    public long addDeviceClass(DeviceClass deviceClass) {
        em.persist(deviceClass);
        return deviceClass.getId();
    }


}
