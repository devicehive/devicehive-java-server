package com.devicehive.service.cache.command;

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
import com.devicehive.model.DeviceCommand;
import org.apache.commons.lang3.StringUtils;
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
public class CommandRedisCacheService implements CommandCacheService {

    private static final Logger log = LoggerFactory.getLogger(CommandRedisCacheService.class);

    private final long cacheTtl;
    private final ValueOperations<String, DeviceCommand> valueOperations;

    public CommandRedisCacheService(
            @Value("${devicehive.cache.commands.ttl.seconds}") long cacheTtl,
            final RedisTemplate<String, DeviceCommand> redisTemplate) {
        this.cacheTtl = cacheTtl;
        this.valueOperations = redisTemplate.opsForValue();
    }

    @Override
    public Optional<DeviceCommand> find(final Long commandId, final String deviceId) {
        log.debug("Searching DeviceCommand by id={} and deviceId={}", commandId, deviceId);
        return find(commandId, deviceId, false);
    }

    @Override
    public Optional<DeviceCommand> find(final Long commandId, final String deviceId, final boolean returnUpdated) {
        log.debug("Searching DeviceCommand by id={}, deviceId={}, updated={}", commandId, deviceId, returnUpdated);
        final String cacheKey = getCacheKey(commandId, deviceId);
        final Optional<DeviceCommand> deviceCommand = Optional.ofNullable(
                valueOperations.get(cacheKey)
        );

        if (returnUpdated) {
            return deviceCommand.filter(command -> Boolean.TRUE.equals(command.getIsUpdated()));
        }

        return deviceCommand;
    }

    @Override
    public Collection<DeviceCommand> find(final Collection<String> deviceIds, final Collection<String> commands,
                                          final Integer take, final Date timestampFrom, final Date timestampTo,
                                          final boolean returnUpdated, final String status) {
        log.debug("Searching DeviceCommand by deviceIds={}, commands={}, from={}, to={}, updated={}, status={}",
                deviceIds, commands, timestampFrom, timestampTo, returnUpdated, status);

        final List<String> cacheKeys = deviceIds.stream().flatMap(deviceId -> getCacheKeys(deviceId).stream()).toList();
        Stream<DeviceCommand> commandsStream = Optional.ofNullable(valueOperations.multiGet(cacheKeys))
                                                       .stream()
                                                       .flatMap(Collection::stream);
        commandsStream = filterByCommands(commandsStream, commands);
        commandsStream = filterByTimestampFrom(commandsStream, timestampFrom);

        if (Objects.nonNull(timestampTo)) {
            commandsStream = commandsStream.filter(command -> {
                final Date timestamp = command.getTimestamp();
                return timestamp.equals(timestampTo) || timestamp.before(timestampTo);
            });
        }

        commandsStream = filterByUpdated(commandsStream, returnUpdated);

        if (StringUtils.isNotBlank(status)) {
            commandsStream = commandsStream.filter(command -> status.equalsIgnoreCase(command.getStatus()));
        }

        return commandsStream
                .limit(take)
                .toList();
    }

    @Override
    public Collection<DeviceCommand> find(final String deviceId, final Collection<Long> networkIds,
                                          final Collection<Long> deviceTypeIds, final Collection<String> commands,
                                          final Integer take, final Date timestampFrom, final boolean returnUpdated) {
        log.debug("Searching DeviceCommand by deviceId={}, networkIds={}, deviceTypeIds={}, commands={}, from={}, updated={}",
                deviceId, networkIds, deviceTypeIds, commands, timestampFrom, returnUpdated);

        final Collection<String> cacheKeys = getCacheKeys(deviceId);
        Stream<DeviceCommand> commandsStream = Optional.ofNullable(valueOperations.multiGet(cacheKeys))
                                                       .stream()
                                                       .flatMap(Collection::stream);
        if (!CollectionUtils.isEmpty(networkIds)) {
            commandsStream = commandsStream.filter(command -> networkIds.contains(command.getNetworkId()));
        }

        if (!CollectionUtils.isEmpty(deviceTypeIds)) {
            commandsStream = commandsStream.filter(command -> deviceTypeIds.contains(command.getDeviceTypeId()));
        }

        commandsStream = filterByCommands(commandsStream, commands);
        commandsStream = filterByTimestampFrom(commandsStream, timestampFrom);
        commandsStream = filterByUpdated(commandsStream, returnUpdated);

        return commandsStream
                .limit(take)
                .toList();
    }

    @Override
    public void store(final DeviceCommand entity) {
        log.debug("Saving DeviceCommand into Redis: {}", entity);
        valueOperations.set(
                entity.getCacheKey(),
                entity,
                Duration.ofSeconds(cacheTtl)
        );
    }

    private String getCacheKey(final Long commandId, final String deviceId) {
        final String id = Objects.isNull(commandId) ? "*" : commandId.toString();
        return String.format("%s_%s_%s", Constants.COMMANDS, deviceId, id);
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

    private Stream<DeviceCommand> filterByCommands(final Stream<DeviceCommand> stream, final Collection<String> commands) {
        if (!CollectionUtils.isEmpty(commands)) {
            return stream.filter(command -> commands.contains(command.getCommand()));
        } else {
            return stream;
        }
    }

    private Stream<DeviceCommand> filterByTimestampFrom(final Stream<DeviceCommand> stream, final Date timestampFrom) {
        if (Objects.nonNull(timestampFrom)) {
            return stream.filter(command -> {
                final Date timestamp = command.getTimestamp();
                return timestamp.equals(timestampFrom) || timestamp.after(timestampFrom);
            });
        } else {
            return stream;
        }

    }

    private Stream<DeviceCommand> filterByUpdated(final Stream<DeviceCommand> stream, boolean updated) {
        if (updated) {
            return stream.filter(command -> Boolean.TRUE.equals(command.getIsUpdated()));
        } else {
            return stream;
        }
    }
}
