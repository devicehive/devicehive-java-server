package com.devicehive.messages.data.derby.subscriptions.dao;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.data.derby.subscriptions.model.CommandUpdatesSubscription;


@Stateless
public class CommandUpdatesSubscriptionDAO {

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CommandUpdatesSubscription getByCommandId(Long id){
        TypedQuery<CommandUpdatesSubscription> query = em.createNamedQuery("CommandUpdateSubscription" +
                ".getByCommandId", CommandUpdatesSubscription.class);
        query.setParameter("commandId", id);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    public void insert(CommandUpdatesSubscription subscription){
        em.persist(subscription);
    }

    public void deleteBySession(String sessionId){
        Query query = em.createNamedQuery("CommandUpdatesSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    public void deleteByCommandId(Long commandId){
        Query query = em.createNamedQuery("CommandUpdatesSubscription.deleteByCommandId");
        query.setParameter("commandId", commandId);
        query.executeUpdate();
    }

}
