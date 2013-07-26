package com.devicehive.messages.data.derby.subscriptions.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.data.derby.subscriptions.model.NotificationsSubscription;
import com.devicehive.model.Device;

@Stateless
public class NotificationSubscriptionDAO {

    private static final Logger logger = LoggerFactory.getLogger(CommandUpdatesSubscriptionDAO.class);

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    public void insertSubscriptions(Collection<Long> deviceIds, String sessionId) {
        if (deviceIds == null) {
            insertSubscriptions(sessionId);
        }
        else if (sessionId != null) {
            for (Long deviceId : deviceIds) {
                deleteByDeviceAndSession(deviceId, sessionId);
                em.persist(new NotificationsSubscription(deviceId, sessionId));
            }
        }
    }

    public void insertSubscriptions(String sessionId) {
        if (sessionId != null) {
            em.persist(new NotificationsSubscription(null, sessionId));
        }
    }

    public void deleteBySession(String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    public void deleteByDeviceAndSession(Long deviceId, String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteByDevicesAndSession");
        query.setParameter("deviceIdList", Arrays.asList(deviceId));
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    public void deleteByDeviceAndSession(Device device, String sessionId) {
        deleteByDeviceAndSession(device.getId(), sessionId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedForAll() {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedForAll", String.class);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedByDevice(Long deviceId) {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedByDevice", String.class);
        query.setParameter("deviceId", deviceId);
        return query.getResultList();
    }

}
