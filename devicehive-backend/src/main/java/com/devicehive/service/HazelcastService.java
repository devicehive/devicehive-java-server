package com.devicehive.service;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HazelcastEntity;
import com.devicehive.service.helpers.HazelcastEntityComparator;
import com.devicehive.service.helpers.HazelcastHelper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class HazelcastService {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastService.class);

    public static final String NOTIFICATIONS_MAP = "NOTIFICATIONS-MAP";
    public static final String COMMANDS_MAP = "COMMANDS-MAP";

    @Autowired
    protected HazelcastInstance hazelcastInstance;

    @Autowired
    protected HazelcastHelper hazelcastHelper;

    private Map<Class, IMap<String, Object>> mapsHolder;

    @PostConstruct
    protected void init() {
        final IMap<String, Object> notificationsMap = hazelcastInstance.getMap("NOTIFICATIONS-MAP");
        notificationsMap.addIndex("timestamp", true);
        final IMap<String, Object> commandsMap = hazelcastInstance.getMap("COMMANDS-MAP");
        commandsMap.addIndex("timestamp", true);

        mapsHolder = new HashMap<>(2);
        mapsHolder.put(DeviceNotification.class, notificationsMap);
        mapsHolder.put(DeviceCommand.class, commandsMap);
    }


    public <T extends HazelcastEntity> Optional<T> find(Long id, String guid, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, guid);
        return retrieve(filters, 1, entityClass).stream().findFirst();
    }

    public <T extends HazelcastEntity> Collection<T> find(Collection<String> devices,
                                                          Collection<String> names,
                                                          Date timestamp,
                                                          String status,
                                                          Integer take,
                                                          Boolean hasResponse,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(devices, names, timestamp, status, hasResponse);
        return retrieve(filters, take, entityClass);
    }

    public <T extends HazelcastEntity> Collection<T> find(Long id,
                                                          String guid,
                                                          Collection<String> devices,
                                                          Collection<String> names,
                                                          Date timestamp, Integer take,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, guid, devices, names, timestamp);
        return retrieve(filters, take, entityClass);
    }

    public <T extends HazelcastEntity> void store(final T hzEntity) {
        logger.debug("Saving entity into hazelcast. [Entity: {}]", hzEntity);
        mapsHolder.get(hzEntity.getClass()).set(hzEntity.getHazelcastKey(), hzEntity);
    }

    @SuppressWarnings("unchecked")
    private <T extends HazelcastEntity> Collection<T> retrieve(Predicate andPredicate, int pageSize, Class<T> tClass) {
        if (pageSize <= 0) {
            final Collection collection = mapsHolder.get(tClass).values(andPredicate);
            return ((Collection<T>) collection);
        } else {
            final PagingPredicate pagingPredicate = new PagingPredicate(andPredicate, new HazelcastEntityComparator(), pageSize);
            final Collection collection = mapsHolder.get(tClass).values(pagingPredicate);
            return ((Collection<T>) collection);
        }
    }
}
