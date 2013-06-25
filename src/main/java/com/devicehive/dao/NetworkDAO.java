package com.devicehive.dao;

import com.devicehive.model.Network;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 24.06.13
 * Time: 17:12
 */
public class NetworkDAO {
    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;

    @Transactional
    public Network findById(Long id) {
        return em.find(Network.class, id);
    }

    @Transactional
    public Network findByName(String name) {
        TypedQuery<Network> query = em.createNamedQuery("Network.findByName", Network.class);
        query.setParameter("name", name);
        List<Network> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional
    public void addNetwork(Network network) {
//        em.refresh(network, LockModeType.PESSIMISTIC_WRITE);
        em.persist(network);
        em.flush();
    }

    @Transactional
    public void updateNetwork(Network network){
        em.refresh(network, LockModeType.PESSIMISTIC_WRITE);
        em.merge(network);
        em.flush();
    }
}
