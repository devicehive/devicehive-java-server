package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HazelcastEntity;
import com.devicehive.service.helpers.HazelcastHelper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;


@Repository
public abstract class AbstractHazelcastEntityService {
    private static final Logger logger = LoggerFactory.getLogger(AbstractHazelcastEntityService.class);

    @Autowired
    protected HazelcastInstance hazelcastInstance;

    @Autowired
    protected HazelcastHelper hazelcastHelper;

    @Autowired
    protected MessageBus messageBus;

    private Map<Class, IMap<String, Object>> mapsHolder;

    @PostConstruct
    protected void init() {
        final IMap<String, Object> notificationsMap = hazelcastInstance.getMap("NOTIFICATIONS-MAP");
        final IMap<String, Object> commandsMap = hazelcastInstance.getMap("COMMANDS-MAP");

        mapsHolder = new HashMap<>(2);
        mapsHolder.put(DeviceNotification.class, notificationsMap);
        mapsHolder.put(DeviceCommand.class, commandsMap);
    }


    protected  <T extends HazelcastEntity> T find(Long id, String guid, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, guid);
        final List<T> entities = new ArrayList<>(retrieve(filters, 1, entityClass));

        return entities.isEmpty() ? null : entities.get(0);
    }

    protected  <T extends HazelcastEntity> Collection<T> find(Collection<String> devices,
                              Collection<String> names,
                              Date timestamp, String status,
                              Integer take, Boolean isUpdated,
                              HivePrincipal principal, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(devices, names, timestamp, status, isUpdated, principal);
        return retrieve(filters, take, entityClass);
    }

    protected  <T extends HazelcastEntity> Collection<T> find(Long id, String guid, Collection<String> devices,
                              Collection<String> names, Date timestamp, Integer take,
                              HivePrincipal principal, Class<T> entityClass) {
        final Predicate filters = hazelcastHelper.prepareFilters(id, guid, devices, names,
                timestamp, principal);
        return retrieve(filters, take, entityClass);
    }

    protected  <T extends HazelcastEntity> void store(final T hzEntity, final Class<T> tClass) {
        logger.debug("Saving entity into hazelcast. [Entity: {}]", hzEntity);
        mapsHolder.get(tClass).put(hzEntity.getHazelcastKey(), hzEntity);
        messageBus.publish(hzEntity);
    }

    @SuppressWarnings("unchecked")
    private  <T extends HazelcastEntity> Collection<T> retrieve(Predicate andPredicate, int pageSize, Class<T> tClass) {
        if (pageSize <= 0) {
            final Collection collection = mapsHolder.get(tClass).values(andPredicate);
            return ((Collection<T>) collection);
        } else {
            final PagingPredicate pagingPredicate = new PagingPredicate(andPredicate, new HazelcastEntityComparator(), pageSize);
            final Collection collection = mapsHolder.get(tClass).values(pagingPredicate);
            return ((Collection<T>) collection);
        }
    }

    private class HazelcastEntityComparator implements Comparator<Map.Entry> {
        @Override
        public int compare(Map.Entry o1, Map.Entry o2) {
            final Date o1Time = ((HazelcastEntity) o1.getValue()).getTimestamp();
            final Date o2Time = ((HazelcastEntity) o2.getValue()).getTimestamp();

            return o2Time.compareTo(o1Time);
        }
    }
}
