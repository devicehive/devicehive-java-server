package com.devicehive.websockets.messagebus.local.subscriptions.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.websockets.messagebus.local.subscriptions.model.NotificationsSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Stateless
public class NotificationSubscriptionDAO {
    private static final Logger logger = LoggerFactory.getLogger(CommandUpdatesSubscriptionDAO.class);
    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    public void insertSubscriptions(Collection<Device> devices, String sessionId) {
        for (Device device : devices) {
            em.persist(new NotificationsSubscription(device.getId(), sessionId));
        }
    }

    public void insertSubscriptions(String sessionId) {
        em.persist(new NotificationsSubscription(null, sessionId));
    }

    public void deleteBySession(String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    public void deleteByDevicesAndSession(String sessionId, Collection<Device> devices) {
        Collection<Long> devicesIds = new LinkedList<>();
        for (Device device : devices) {
            devicesIds.add(device.getId());
        }
        Query query = em.createNamedQuery("NotificationsSubscription.deleteByDevicesAndSession");
        query.setParameter("deviceIdList", devicesIds);
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedForAll() {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedForAll", String.class);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedByDevice(Long deviceId) {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedByDevice",
                String.class);
        query.setParameter("deviceId", deviceId);
        return query.getResultList();
    }

}
