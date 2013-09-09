package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Stateless
public class AccessKeyPermissionDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public AccessKeyPermission insert(AccessKeyPermission permission){
        em.persist(permission);
        return permission;
    }

    public int deleteByAccessKey(AccessKey accessKey){
        Query query = em.createNamedQuery("AccessKeyPermission.deleteByAccessKey");
        query.setParameter("accessKey", accessKey);
        return query.executeUpdate();
    }
}
