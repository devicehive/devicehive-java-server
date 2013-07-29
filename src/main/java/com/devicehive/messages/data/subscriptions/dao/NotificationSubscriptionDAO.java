package com.devicehive.messages.data.subscriptions.dao;

import com.devicehive.model.Device;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collection;
import java.util.List;

public interface NotificationSubscriptionDAO {

    public void insertSubscriptions(Collection<Long> deviceIds, String sessionId);

    public void insertSubscriptions(Long deviceId, String sessionId);
    
    public void insertSubscriptions(String sessionId);

    public void deleteBySession(String sessionId);

    public void deleteByDeviceAndSession(Long deviceId, String sessionId);

    public void deleteByDeviceAndSession(Device device, String sessionId);

    public void deleteByDevice(Long deviceId);

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedForAll();

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedByDevice(Long deviceId);

}
