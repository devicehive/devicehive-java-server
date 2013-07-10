package com.devicehive.websockets.messagebus.local.subscriptions.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandsSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.websocket.Session;


public class CommandSubscriptionDAO {
    private static final Logger logger = LoggerFactory.getLogger(CommandSubscriptionDAO.class);

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    public CommandsSubscription getById(Long id){
        return em.find(CommandsSubscription.class, id);
    }

    public CommandsSubscription getByDeviceId(Long id){
        TypedQuery<CommandsSubscription> query = em.createNamedQuery("CommandsSubscription.getByDeviceId",
                CommandsSubscription.class);
        query.setParameter("deviceId", id);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @Transactional
    public void insert(CommandsSubscription subscription){
        em.persist(subscription);
    }

    @Transactional
    public void deleteBySession(String sessionId){
        Query query = em.createNamedQuery("CommandsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    @Transactional
    public void deleteByDeviceAndSession(Long deviceId, String sessionId){
        Query query = em.createNamedQuery("CommandsSubscription.deleteByDeviceAndSession");
        query.setParameter("sessionId", sessionId);
        query.setParameter("deviceId", deviceId);
        query.executeUpdate();
    }
}
