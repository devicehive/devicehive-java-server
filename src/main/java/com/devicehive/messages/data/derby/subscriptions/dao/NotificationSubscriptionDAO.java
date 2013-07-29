package com.devicehive.messages.data.derby.subscriptions.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.data.subscriptions.model.NotificationsSubscription;
import com.devicehive.model.Device;

@Dependent
public class NotificationSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.NotificationSubscriptionDAO {

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;
    
    public NotificationSubscriptionDAO() {}

    @Override
    public void insertSubscriptions(Collection<Long> deviceIds, String sessionId) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            insertSubscriptions(sessionId);
        }
        else if (sessionId != null) {
            for (Long deviceId : deviceIds) {
                deleteByDeviceAndSession(deviceId, sessionId);
                em.persist(new NotificationsSubscription(deviceId, sessionId));
            }
        }
    }

    @Override
    public void insertSubscriptions(Long deviceId, String sessionId) {
        if (sessionId != null) {
            em.persist(new NotificationsSubscription(null, sessionId));
        }
    }

    @Override
    public void insertSubscriptions(String sessionId) {
        insertSubscriptions((Long) null, sessionId);
    }

    @Override
    public void deleteBySession(String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    @Override
    public void deleteByDeviceAndSession(Long deviceId, String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteByDevicesAndSession");
        query.setParameter("deviceIdList", Arrays.asList(deviceId));
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    @Override
    public void deleteByDeviceAndSession(Device device, String sessionId) {
        deleteByDeviceAndSession(device.getId(), sessionId);
    }

    @Override
    public void deleteByDevice(Long deviceId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteByDevice");
        query.setParameter("deviceId", deviceId);
        query.executeUpdate();
    }

    @Override
    public List<String> getSessionIdSubscribedForAll() {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedForAll", String.class);
        return query.getResultList();
    }

    @Override
    public List<String> getSessionIdSubscribedByDevice(Long deviceId) {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedByDevice", String.class);
        query.setParameter("deviceId", deviceId);
        return query.getResultList();
    }

}
