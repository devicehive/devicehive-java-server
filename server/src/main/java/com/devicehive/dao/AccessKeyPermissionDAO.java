package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKeyPermission;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AccessKeyPermissionDAO {
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public AccessKeyPermission insert(AccessKeyPermission permission){
        em.persist(permission);
        return permission;
    }
}
