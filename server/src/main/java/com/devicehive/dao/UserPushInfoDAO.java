package com.devicehive.dao;


import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.devicehive.configuration.Constants;
import com.devicehive.model.UserPushInfo;
import com.devicehive.util.LogExecutionTime;

import static com.devicehive.model.UserPushInfo.Queries.Names.*;
import static com.devicehive.model.UserPushInfo.Queries.Parameters.*;

@Stateless
@LogExecutionTime
public class UserPushInfoDAO {

	@PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;
	
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public UserPushInfo findByUserId(Long userId) {
        TypedQuery<UserPushInfo> query = em.createNamedQuery(FIND_BY_USER_ID, UserPushInfo.class);
        query.setParameter(USER_ID, userId);
        CacheHelper.cacheable(query);
        List<UserPushInfo> userPushInfos = query.getResultList();
        return userPushInfos.isEmpty() ? null : userPushInfos.get(0);
    }
    
    public UserPushInfo findByUserIdRegId(Long userId, String regId) {
    	TypedQuery<UserPushInfo> query = em.createNamedQuery(FIND_BY_USER_ID_REG_ID, UserPushInfo.class);
    	query.setParameter(USER_ID, userId);
    	query.setParameter(REG_ID, regId);
    	CacheHelper.cacheable(query);
        List<UserPushInfo> userPushInfos = query.getResultList();
        return userPushInfos.isEmpty() ? null : userPushInfos.get(0);
    }
	
    public UserPushInfo create(UserPushInfo userPushInfo) {
        em.persist(userPushInfo);
        return userPushInfo;
    }

    public UserPushInfo update(@NotNull @Valid UserPushInfo userPushInfo) {
        em.merge(userPushInfo);
        return userPushInfo;
    }
}
