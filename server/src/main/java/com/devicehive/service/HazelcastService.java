package com.devicehive.service;


import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Singleton
@ConcurrencyManagement(BEAN)
@Startup
public class HazelcastService {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastService.class);

    private HazelcastInstance hazelcast;

    @PostConstruct
    protected void postConstruct() {
        logger.debug("Initializing Hazelcast instance...");
        hazelcast = Hazelcast.newHazelcastInstance();
        logger.debug("New Hazelcast instance created: " + hazelcast);

    }

    @PreDestroy
    protected void preDestroy() {
        hazelcast.getLifecycleService().shutdown();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public HazelcastInstance getHazelcast() {
        return hazelcast;
    }
}
