package com.devicehive.service.cache.notification;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class NotificationRedisCacheService implements NotificationCacheService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisCacheService.class);

    private final long cacheTtl;
    private final ValueOperations<String, DeviceNotification> valueOperations;

    public NotificationRedisCacheService(
            @Value("${devicehive.cache.notifications.ttl.seconds}") long cacheTtl,
            final RedisTemplate<String, DeviceNotification> redisTemplate) {
        this.cacheTtl = cacheTtl;
        this.valueOperations = redisTemplate.opsForValue();
    }

    @Override
    public Optional<DeviceNotification> find(final Long notificationId, final String deviceId) {
        log.debug("Searching DeviceNotification by id={} and deviceId={}", notificationId, deviceId);
        final String cacheKey = getCacheKey(notificationId, deviceId);
        return Optional.ofNullable(
                valueOperations.get(cacheKey)
        );
    }

    @Override
    public Collection<DeviceNotification> find(final Collection<String> deviceIds, final Collection<String> notifications,
                                               final Integer take, final Date timestampFrom, final Date timestampTo) {
        log.debug("Searching DeviceNotification by deviceIds={}, notifications={}, from={}, to={}",
                deviceIds, notifications, timestampFrom, timestampTo);

        final List<String> cacheKeys = deviceIds.stream().flatMap(deviceId -> getCacheKeys(deviceId).stream()).toList();
        Stream<DeviceNotification> notificationsStream = Optional.ofNullable(valueOperations.multiGet(cacheKeys))
                                                                 .stream()
                                                                 .flatMap(Collection::stream);

        notificationsStream = filterByNotifications(notificationsStream, notifications);
        notificationsStream = filterByTimestampFrom(notificationsStream, timestampFrom);

        if (Objects.nonNull(timestampTo)) {
            notificationsStream = notificationsStream.filter(notification -> {
                final Date timestamp = notification.getTimestamp();
                return timestamp.equals(timestampTo) || timestamp.before(timestampTo);
            });
        }

        return notificationsStream
                .limit(take)
                .toList();
    }

    @Override
    public Collection<DeviceNotification> find(final String deviceId, final Collection<Long> networkIds,
                                               final Collection<Long> deviceTypeIds, final Collection<String> notifications,
                                               final Integer take, final Date timestampFrom) {
        log.debug("Searching DeviceNotification by deviceId={}, networkIds={}, deviceTypeIds={}, notifications={}, from={}",
                deviceId, networkIds, deviceTypeIds, notifications, timestampFrom);

        final Collection<String> cacheKeys = getCacheKeys(deviceId);
        Stream<DeviceNotification> notificationStream = Optional.ofNullable(valueOperations.multiGet(cacheKeys))
                                                                .stream()
                                                                .flatMap(Collection::stream);

        if (!CollectionUtils.isEmpty(networkIds)) {
            notificationStream = notificationStream.filter(notification -> networkIds.contains(notification.getNetworkId()));
        }

        if (!CollectionUtils.isEmpty(deviceTypeIds)) {
            notificationStream = notificationStream.filter(notification -> deviceTypeIds.contains(notification.getDeviceTypeId()));
        }

        notificationStream = filterByNotifications(notificationStream, notifications);
        notificationStream = filterByTimestampFrom(notificationStream, timestampFrom);

        return notificationStream
                .limit(take)
                .toList();
    }

    @Override
    public void store(final DeviceNotification entity) {
        log.debug("Saving DeviceNotification into Redis: {}", entity);
        valueOperations.set(
                entity.getCacheKey(),
                entity,
                Duration.ofSeconds(cacheTtl)
        );
    }

    private String getCacheKey(final Long notificationId, final String deviceId) {
        final String id = Objects.isNull(notificationId) ? "*" : notificationId.toString();
        return String.format("%s_%s_%s", Constants.NOTIFICATIONS, deviceId, id);
    }

    private Collection<String> getCacheKeys(final String deviceId) {
        final ScanOptions scanOptions = ScanOptions.scanOptions()
                                                   .match(getCacheKey(null, deviceId))
                                                   .build();
        try (
                final Cursor<String> cursor = valueOperations.getOperations().scan(scanOptions)
        ) {
            return cursor.stream().toList();
        }
    }

    private Stream<DeviceNotification> filterByNotifications(final Stream<DeviceNotification> stream, final Collection<String> notifications) {
        if (!CollectionUtils.isEmpty(notifications)) {
            return stream.filter(notification -> notifications.contains(notification.getNotification()));
        } else {
            return stream;
        }
    }

    private Stream<DeviceNotification> filterByTimestampFrom(final Stream<DeviceNotification> stream, final Date timestampFrom) {
        if (Objects.nonNull(timestampFrom)) {
            return stream.filter(notification -> {
                final Date timestamp = notification.getTimestamp();
                return timestamp.equals(timestampFrom) || timestamp.after(timestampFrom);
            });
        } else {
            return stream;
        }
    }
}
