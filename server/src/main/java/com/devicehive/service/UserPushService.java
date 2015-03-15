package com.devicehive.service;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.UserPushInfoDAO;
import com.devicehive.model.UserPushInfo;
import com.devicehive.model.enums.PushRegisterStatus;
import com.devicehive.model.updates.UserPushInfoUpdate;

@Stateless
@EJB(beanInterface = UserPushService.class, name = "UserPushService")
public class UserPushService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserPushService.class);
	
	@EJB
	private UserPushInfoDAO pushInfoDAO;
	
	@PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public UserPushInfo saveUserPushInfo(Long userId, UserPushInfoUpdate userPushInfoUpdate) {
		String regId = "";
		UserPushInfo userPushInfo = null;
		
		if (userPushInfoUpdate.getRegId() == null) {
			// 없으면 안됨... 필히 regId는 있어야함
		} else {
			regId = userPushInfoUpdate.getRegId().getValue();
			// find UserPushInfo with same regid
			userPushInfo = pushInfoDAO.findByUserIdRegId(userId, regId);
			
			// update | create classify
			if (userPushInfo != null) {
				
				userPushInfo.setUserId(userId);
				userPushInfo.setOsType(userPushInfoUpdate.getOsType().getValue());
				userPushInfo.setVersion(userPushInfoUpdate.getVersion().getValue());
				userPushInfo.setRegId(regId);
				userPushInfo.setStatus(PushRegisterStatus.REGISTERED);
				
				pushInfoDAO.update(userPushInfo);
			} else {
				userPushInfo = new UserPushInfo();
				
				userPushInfo.setUserId(userId);
				userPushInfo.setOsType(userPushInfoUpdate.getOsType().getValue());
				userPushInfo.setVersion(userPushInfoUpdate.getVersion().getValue());
				userPushInfo.setRegId(regId);
				userPushInfo.setStatus(PushRegisterStatus.REGISTERED);
				
				pushInfoDAO.create(userPushInfo);
			}
			
		}
		
		return userPushInfo;
	}
	
	
	
}
