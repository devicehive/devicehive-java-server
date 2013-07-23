package com.devicehive.websockets.messagebus.local.subscriptions.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.websockets.messagebus.local.subscriptions.model.CommandsSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.websocket.Session;

@Stateless
public class CommandSubscriptionDAO {
    private static final Logger logger = LoggerFactory.getLogger(CommandSubscriptionDAO.class);

    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CommandsSubscription getByDeviceId(Long id){
        TypedQuery<CommandsSubscription> query = em.createNamedQuery("CommandsSubscription.getByDeviceId",
                CommandsSubscription.class);
        query.setParameter("deviceId", id);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    public void insert(CommandsSubscription subscription){
        em.persist(subscription);
    }

    public void deleteBySession(String sessionId){
        Query query = em.createNamedQuery("CommandsSubscription.deleteBySession");
        query.setParameter("sessionId", sessionId);
        query.executeUpdate();
    }

    public void deleteByDevice(Long deviceId){
        Query query = em.createNamedQuery("CommandsSubscription.deleteByDevice");
        query.setParameter("deviceId", deviceId);
        query.executeUpdate();
    }
}
