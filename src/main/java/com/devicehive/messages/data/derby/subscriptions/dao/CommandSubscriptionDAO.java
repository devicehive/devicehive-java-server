package com.devicehive.messages.data.derby.subscriptions.dao;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

@Dependent
public class CommandSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO {

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    public CommandSubscriptionDAO() {}

    @Override
    public CommandsSubscription getByDeviceId(Long id) {
        TypedQuery<CommandsSubscription> query = em.createNamedQuery("CommandsSubscription.getByDeviceId",
                CommandsSubscription.class);
        query.setParameter("deviceId", id);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @Override
    public void insert(CommandsSubscription subscription) {
        em.persist(subscription);
    }

    @Override
    public void deleteBySession(String sessionId) {
        Query query = em.createNamedQuery("CommandsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    @Override
    public void deleteByDevice(Long deviceId) {
        Query query = em.createNamedQuery("CommandsSubscription.deleteByDevice");
        query.setParameter("deviceId", deviceId);
        query.executeUpdate();
    }
}
