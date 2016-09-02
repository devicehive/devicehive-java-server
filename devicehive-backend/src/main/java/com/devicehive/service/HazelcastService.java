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

    private Map<Class, IMap<String, HazelcastEntity>> mapsHolder = new HashMap<>(2);

    @PostConstruct
    protected void init() {
        final IMap<String, HazelcastEntity> notificationsMap = hazelcastInstance.getMap(NOTIFICATIONS_MAP);
        notificationsMap.addIndex("timestamp", true);

        final IMap<String, HazelcastEntity> commandsMap = hazelcastInstance.getMap(COMMANDS_MAP);
        commandsMap.addIndex("timestamp", true);

        mapsHolder.put(DeviceNotification.class, notificationsMap);
        mapsHolder.put(DeviceCommand.class, commandsMap);
    }


    public <T extends HazelcastEntity> Optional<T> find(Long id, String guid, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, guid);
        return find(filters, 1, entityClass).stream().findFirst();
    }

    public <T extends HazelcastEntity> Collection<T> find(String guid,
                                                          Collection<String> names,
                                                          Date timestampSt,
                                                          Date timestampEnd,
                                                          String status,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(guid,  names, timestampSt, timestampEnd, status);
        return find(filters, 0, entityClass);
    }

    public <T extends HazelcastEntity> Collection<T> find(String guid,
                                                          Collection<String> names,
                                                          Date timestampSt,
                                                          Date timestampEnd,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(guid,  names, timestampSt, timestampEnd);
        return find(filters, 0, entityClass);
    }

    public <T extends HazelcastEntity> Collection<T> find(Long id,
                                                          String guid,
                                                          Collection<String> devices,
                                                          Collection<String> names,
                                                          Date timestamp, Integer take,
                                                          Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, guid, devices, names, timestamp);
        return find(filters, take, entityClass);
    }

    public <T extends HazelcastEntity> void store(final T hzEntity) {
        logger.debug("Saving entity into hazelcast. [Entity: {}]", hzEntity);
        mapsHolder.get(hzEntity.getClass()).set(hzEntity.getHazelcastKey(), hzEntity);
    }

    @SuppressWarnings("unchecked")
    private <T extends HazelcastEntity> Collection<T> find(Predicate predicate, int pageSize, Class<T> tClass) {
        final Predicate pagingPredicate = (pageSize > 0)
                ? new PagingPredicate(predicate, new HazelcastEntityComparator(), pageSize)
                : predicate;
        return (Collection<T>) mapsHolder.get(tClass).values(pagingPredicate);
    }
}
