package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import static com.devicehive.model.AccessKeyPermission.Queries.Names.DELETE_BY_ACCESS_KEY;
import static com.devicehive.model.AccessKeyPermission.Queries.Parameters.ACCESS_KEY;

@Stateless
public class AccessKeyPermissionDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public AccessKeyPermission insert(AccessKeyPermission permission){
        em.persist(permission);
        return permission;
    }

    public int deleteByAccessKey(AccessKey accessKey){
        Query query = em.createNamedQuery(DELETE_BY_ACCESS_KEY);
        query.setParameter(ACCESS_KEY, accessKey);
        return query.executeUpdate();
    }
}
