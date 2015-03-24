package com.devicehive.service;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserPushInfoDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.UserPushInfo;
import com.devicehive.model.enums.PushRegisterStatus;
import com.devicehive.model.updates.UserPushInfoUpdate;

@Stateless
@EJB(beanInterface = UserPushService.class, name = "UserPushService")
public class UserPushService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserPushService.class);
	
	@EJB
	private UserPushInfoDAO pushInfoDAO;
	@EJB
    private NetworkDAO networkDAO;
	@EJB
    private DeviceDAO deviceDAO;
	
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
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<UserPushInfo> getListByNetworkId(String uuid) {
		List<UserPushInfo> userPushInfos = null;
		
		Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(uuid);
		
		Long networkId = device.getNetwork().getId();
		
		Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork == null) {
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
        Set<User> usersSet = existingNetwork.getUsers();
        if (usersSet != null && usersSet.size() > 0) {
        	userPushInfos = new ArrayList<UserPushInfo>();
	        Iterator<User> iter = usersSet.iterator();
	        while (iter.hasNext()) {
	            User user = iter.next();
	            List<UserPushInfo> pushInfos = pushInfoDAO.findByUserId(user.getId());
	            for (UserPushInfo upi : pushInfos) 
	            	userPushInfos.add(upi);
	        }
        }
        
        return userPushInfos;
	}
	
}
