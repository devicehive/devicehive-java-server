package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Stateless
@LogExecutionTime
public class RedisCommandService {
    private static final String KEY_FORMAT = "command:%s:%s:%s";

    @EJB
    private RedisConnector redis;
    @EJB
    private PropertiesService propertiesService;

    public void save(DeviceCommand deviceCommand) {
        save(deviceCommand, getKey(deviceCommand.getId(), deviceCommand.getDeviceGuid(), deviceCommand.getTimestamp()));
    }

    public void update(DeviceCommand deviceCommand) {
        final DeviceCommand existing = get(deviceCommand);
        if (existing != null) {
            if (deviceCommand.getCommand() == null) {
                deviceCommand.setCommand(existing.getCommand());
            }
            save(deviceCommand, getKey(deviceCommand.getId(), deviceCommand.getDeviceGuid(), existing.getTimestamp()));
        }
    }

    public DeviceCommand getByIdAndGuid(final Long id, final String guid) {
        SortedSet<DeviceCommand> commands = redis.fetch(String.format(KEY_FORMAT, guid, id, "*"), Constants.DEFAULT_TAKE, new DeviceCommandComparator(),
                new Transformer<String, DeviceCommand>() {
                    @Override
                    public DeviceCommand apply(String key) {
                        return get(key, false, false, null, null, null, true);
                    }
                });
        return !(commands == null || commands.isEmpty()) ? commands.first() : null;
    }

    public Collection<DeviceCommand> getByGuids(Collection<String> guids, final Collection<String> names,
                                                final Timestamp timestamp, final String status, final Integer take, final Boolean isUpdated) {
        final boolean filterByDate = timestamp != null;
        final boolean filterByName = CollectionUtils.isNotEmpty(names);
        return getAllKeysByGuids(guids, take, new Transformer<String, DeviceCommand>() {
            @Override
            public DeviceCommand apply(String key) {
                return get(key, filterByDate, filterByName, names, timestamp, status, isUpdated);
            }
        });
    }

    public Collection<DeviceCommand> getAll(final Collection<String> names, final Timestamp timestamp,
                                            final String status, final Integer take, final Boolean isUpdated) {
        final boolean filterByDate = timestamp != null;
        final boolean filterByName = CollectionUtils.isNotEmpty(names);
        return redis.fetch(String.format(KEY_FORMAT, "*", "*", "*"), take, new DeviceCommandComparator(), new Transformer<String, DeviceCommand>() {
            @Override
            public DeviceCommand apply(String key) {
                return get(key, filterByDate, filterByName, names, timestamp, status, isUpdated);
            }
        });
    }

    private void save(DeviceCommand deviceCommand, final String key) {
        Map<String, String> commandMap = new HashMap<>();
        commandMap.put("command", deviceCommand.getCommand() != null ? deviceCommand.getCommand() : "");
        commandMap.put("id", deviceCommand.getId().toString());
        commandMap.put("deviceGuid", deviceCommand.getDeviceGuid());
        commandMap.put("isUpdated", deviceCommand.getIsUpdated().toString());
        commandMap.put("parameters", (deviceCommand.getParameters() != null && deviceCommand.getParameters().getJsonString() != null)
                ? deviceCommand.getParameters().getJsonString() : "");
        commandMap.put("timestamp", deviceCommand.getTimestamp() != null ? TimestampAdapter.formatTimestamp(deviceCommand.getTimestamp()) : "");
        commandMap.put("userId", deviceCommand.getUserId() != null ? deviceCommand.getUserId().toString() : "");
        commandMap.put("lifetime", deviceCommand.getLifetime() != null ? deviceCommand.getLifetime().toString() : "");
        commandMap.put("flags", deviceCommand.getFlags() != null ? deviceCommand.getFlags().toString() : "");
        commandMap.put("result", (deviceCommand.getResult() != null && deviceCommand.getResult().getJsonString() != null)
                ? deviceCommand.getResult().getJsonString() : "");
        commandMap.put("status", deviceCommand.getStatus() != null ? deviceCommand.getStatus() : "");
        redis.setAll(key, commandMap, propertiesService.getProperty(Constants.COMMAND_EXPIRE_SEC));
    }

    private DeviceCommand get(DeviceCommand deviceCommand) {
        final DeviceCommand command = getByIdAndGuid(deviceCommand.getId(), deviceCommand.getDeviceGuid());
        if (command != null && !command.getIsUpdated()) {
            return command;
        }
        return null;
    }

    private DeviceCommand get(String key, final boolean filterByDate, final boolean filterByName, final Collection<String> names,
                              final Timestamp timestamp, final String status, final Boolean isUpdated) {
        Map<String, String> commandMap = redis.getAll(key);
        if (commandMap != null && !commandMap.isEmpty()) {
            final Timestamp commandTimestamp = TimestampAdapter.parseTimestamp(commandMap.get("timestamp"));
            final boolean skip = (filterByDate && commandTimestamp.compareTo(timestamp) <= 0 || (filterByName && !names.contains(commandMap.get("command"))) ||
                    (!isUpdated && Boolean.valueOf(commandMap.get("isUpdated"))) || (StringUtils.isNotEmpty(status) && !status.equals(commandMap.get("status"))));
            if (!skip) {
                DeviceCommand command = new DeviceCommand();
                command.setId(Long.valueOf(commandMap.get("id")));
                command.setDeviceGuid(commandMap.get("deviceGuid"));
                command.setCommand(commandMap.get("command"));
                command.setParameters(new JsonStringWrapper(commandMap.get("parameters")));
                command.setTimestamp(commandTimestamp);
                if (StringUtils.isNoneEmpty(commandMap.get("userId"))) {
                    command.setUserId(Long.parseLong(commandMap.get("userId")));
                }
                if (StringUtils.isNoneEmpty(commandMap.get("lifetime"))) {
                    command.setLifetime(Integer.parseInt(commandMap.get("lifetime")));
                }
                if (StringUtils.isNoneEmpty(commandMap.get("flags"))) {
                    command.setFlags(Integer.parseInt(commandMap.get("flags")));
                }
                if (StringUtils.isNoneEmpty(commandMap.get("result"))) {
                    command.setResult(new JsonStringWrapper(commandMap.get("result")));
                }
                if (StringUtils.isNoneEmpty(commandMap.get("status"))) {
                    command.setStatus(commandMap.get("status"));
                }
                if (commandMap.get("isUpdated") != null) {
                    command.setIsUpdated(Boolean.valueOf(commandMap.get("isUpdated")));
                }
                return command;
            }
        }
        return null;
    }

    private Set<DeviceCommand> getAllKeysByGuids(Collection<String> guids, Integer count, Transformer<String, DeviceCommand> transformer) {
        if (CollectionUtils.isNotEmpty(guids)) {
            Comparator<DeviceCommand> comparator = new DeviceCommandComparator();
            Set<DeviceCommand> accumulator = new TreeSet<>(comparator);
            for (String guid : guids) {
                accumulator.addAll(
                        redis.fetch(String.format(KEY_FORMAT, guid, "*", "*"), count, comparator, transformer)
                );
            }
            if (accumulator.size() > count) {
                List<DeviceCommand> sliced = new ArrayList<>(accumulator).subList(0, count);
                accumulator.clear();
                accumulator.addAll(sliced);
            }
            return accumulator;
        }
        return Collections.emptySet();
    }

    private String getKey(final Long id, final String guid, final Timestamp timestamp) {
        return String.format(KEY_FORMAT, guid, id, timestamp);
    }

    private class DeviceCommandComparator implements Comparator<DeviceCommand> {
        @Override
        public int compare(DeviceCommand o1, DeviceCommand o2) {
            return o2.getTimestamp().compareTo(o1.getTimestamp());
        }
    }
}
