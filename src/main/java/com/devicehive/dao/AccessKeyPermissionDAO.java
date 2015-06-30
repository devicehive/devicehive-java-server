package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import static com.devicehive.model.AccessKeyPermission.Queries.Names.DELETE_BY_ACCESS_KEY;
import static com.devicehive.model.AccessKeyPermission.Queries.Parameters.ACCESS_KEY;

@Component
public class AccessKeyPermissionDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public AccessKeyPermission insert(AccessKeyPermission permission) {
        em.persist(permission);
        return permission;
    }

    @Transactional
    public int deleteByAccessKey(AccessKey accessKey) {
        Query query = em.createNamedQuery(DELETE_BY_ACCESS_KEY);
        query.setParameter(ACCESS_KEY, accessKey);
        return query.executeUpdate();
    }
}
