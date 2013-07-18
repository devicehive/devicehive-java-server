package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Network;
import com.devicehive.service.interceptors.ValidationInterceptor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

@Stateless
@Interceptors(ValidationInterceptor.class)
public class NetworkDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public Network createNetwork(Network network) {
        em.persist(network);
        return network;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network getById(@NotNull long id) {
        return em.find(Network.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network findByName(@NotNull String name) {
        TypedQuery<Network> query = em.createNamedQuery("Network.findByName", Network.class);
        query.setParameter("name", name);
        List<Network> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
    private static final Logger logger = LoggerFactory.getLogger(NetworkDAO.class);

    public Network getByIdWithUsers(@NotNull long id) {
        Network result = em.find(Network.class,id);
        Hibernate.initialize(result.getUsers());
        return result;
    }

}
