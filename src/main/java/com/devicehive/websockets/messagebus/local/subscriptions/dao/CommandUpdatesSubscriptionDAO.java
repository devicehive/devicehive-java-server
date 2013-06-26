package com.devicehive.websockets.messagebus.local.subscriptions.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandUpdatesSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;


public class CommandUpdatesSubscriptionDAO {
    private static final Logger logger = LoggerFactory.getLogger(CommandUpdatesSubscriptionDAO.class);

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    public CommandUpdatesSubscription getById(Long id){
        return em.find(CommandUpdatesSubscription.class, id);
    }

    @Transactional
    public void insert(CommandUpdatesSubscription subscription){
        em.persist(subscription);
        em.flush();
    }

    @Transactional
    public void deleteBySession(String sessionId){
        Query query = em.createNamedQuery("CommandUpdatesSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

}
