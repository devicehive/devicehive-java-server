package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Network;
import com.devicehive.service.interceptors.ValidationInterceptor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

/**
 * @author Nikolay Loboda <madlooser@gmail.com>
 * @since 7/18/13 2:27 AM
 */
@Stateless
@Interceptors(ValidationInterceptor.class)
public class NetworkDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    private static final Logger logger = LoggerFactory.getLogger(NetworkDAO.class);

    public Network getById(@NotNull long id) {
        return em.find(Network.class,id);
    }

    public Network getByIdWithUsers(@NotNull long id) {
        Network result = em.find(Network.class,id);
        Hibernate.initialize(result.getUsers());
        return result;
    }

}
