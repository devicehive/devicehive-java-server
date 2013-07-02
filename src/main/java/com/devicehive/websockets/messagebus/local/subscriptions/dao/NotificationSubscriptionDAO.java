package com.devicehive.websockets.messagebus.local.subscriptions.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.websockets.messagebus.local.subscriptions.model.NotificationsSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.Collection;
import java.util.List;


public class NotificationSubscriptionDAO {
    private static final Logger logger = LoggerFactory.getLogger(CommandUpdatesSubscriptionDAO.class);
    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    public NotificationsSubscription getById(Long id) {
        return em.find(NotificationsSubscription.class, id);
    }

    @Transactional
    public void insertSubscription(NotificationSubscriptionDAO subscription) {
        em.persist(subscription);
        em.flush();
    }

    @Transactional
    public void insertSubscriptions(Collection<Device> devices, String sessionId) {
        for (Device device : devices) {
            em.persist(new NotificationsSubscription(device.getId(), sessionId));
        }
        em.flush();
    }

    @Transactional
    public void insertSubscriptions(String sessionId) {
        em.persist(new NotificationsSubscription(null, sessionId));
    }

    @Transactional
    public void deleteBySession(String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    @Transactional
    public void deleteByDeviceAndSession(Device device, String sessionId) {
        Query query = em.createNamedQuery("NotificationsSubscription.deleteByDeviceAndSession");
        query.setParameter("deviceId", device.getId());
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    public List<String> getSessionIdSubscribedForAll() {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedForAll", String.class);
        return query.getResultList();
    }

    public List<String> getSessionIdSubscribedByDevice(Long deviceId) {
        TypedQuery<String> query = em.createNamedQuery("NotificationsSubscription.getSubscribedByDevice",
                String.class);
        return query.getResultList();
    }

    private List<NotificationsSubscription> getAll() {
        TypedQuery<NotificationsSubscription> query = em.createNamedQuery("NotificationsSubscription.selectAll",
                NotificationsSubscription.class);
        return query.getResultList();
    }
}
