package com.devicehive.service;


import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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

    private boolean wasCreated = false;

    @PostConstruct
    protected void postConstruct() {
        logger.info("Initializing Hazelcast instance...");
        Set<HazelcastInstance> set = Hazelcast.getAllHazelcastInstances();
        if (!set.isEmpty()) {
            hazelcast = set.iterator().next();
            logger.info("Existing Hazelcast instance is reused: " + hazelcast);
        } else {
            hazelcast = Hazelcast.newHazelcastInstance();
            wasCreated = true;
            logger.info("New Hazelcast instance created: " + hazelcast);
        }
    }

    @PreDestroy
    protected void preDestroy() {
        if (wasCreated) {
            hazelcast.getLifecycleService().shutdown();
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public HazelcastInstance getHazelcast() {
        return hazelcast;
    }
}
