package com.devicehive.auth.rest;

import com.devicehive.configuration.Constants;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class ResponseFilter implements ContainerResponseFilter {
    private static final Logger logger = LoggerFactory.getLogger(ResponseFilter.class);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;
    private EntityManagerFactoryImpl emf;

    @PostConstruct
    public void init() {
        emf = (EntityManagerFactoryImpl) em.getEntityManagerFactory();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        logger.info("Hibernate statistic -> \n    {}", emf.getSessionFactory().getStatistics());
    }
}
