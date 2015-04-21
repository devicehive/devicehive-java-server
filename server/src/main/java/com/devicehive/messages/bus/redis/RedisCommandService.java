package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
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

    public DeviceCommand get(DeviceCommand deviceCommand) {
        final String key = String.format(KEY_FORMAT, deviceCommand.getDeviceGuid(), deviceCommand.getId(), "false");
        return getByKey(key);
    }

    @Asynchronous
    public void save(DeviceCommand deviceCommand) {
        final String key = String.format(KEY_FORMAT, deviceCommand.getDeviceGuid(), deviceCommand.getId(), deviceCommand.getIsUpdated());
        Map<String, String> commandMap = new HashMap<>();
        commandMap.put("command", deviceCommand.getCommand());
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

    public DeviceCommand getByKey(String key) {
        Map<String, String> commandMap = redis.getAll(key);
        if (commandMap != null && !commandMap.isEmpty()) {
            DeviceCommand command = new DeviceCommand();
            command.setId(Long.valueOf(commandMap.get("id")));
            command.setDeviceGuid(commandMap.get("deviceGuid"));
            command.setCommand(commandMap.get("command"));
            command.setParameters(new JsonStringWrapper(commandMap.get("parameters")));
            command.setTimestamp(TimestampAdapter.parseTimestamp(commandMap.get("timestamp")));
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
        return null;
    }

    public DeviceCommand getByIdAndGuid(final Long id, final String guid) {
        final List<String> keys = Arrays.asList(String.format(KEY_FORMAT, guid, id, "true"),
                    String.format(KEY_FORMAT, guid, id, "false"));
        List<DeviceCommand> commands = new ArrayList<>();
        for (final String key : keys) {
            final DeviceCommand command = getByKey(key);
            if (command != null) {
                commands.add(getByKey(key));
            }
        }
        return !commands.isEmpty() ? commands.get(0) : null;
    }

    public List<DeviceCommand> getByGuids(Collection<String> guids, final Boolean isUpdated) {
        final List<String> keys = getAllKeysByGuids(guids, isUpdated);
        if (CollectionUtils.isNotEmpty(keys)) {
            List<DeviceCommand> commands = new ArrayList<>();
            for (final String key : keys) {
                final DeviceCommand command = getByKey(key);
                if (command != null) {
                    commands.add(getByKey(key));
                }
            }
            return commands;
        }
        return Collections.emptyList();
    }

    public List<String> getAllKeysByGuids(Collection<String> guids, final Boolean isUpdated) {
        final String isUpdatedKeyPart = isUpdated != null ? isUpdated.toString() : "*";
        if (CollectionUtils.isNotEmpty(guids)) {
            List<String> keys = new ArrayList<>();
            for (String guid : guids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, guid, "*", isUpdatedKeyPart)));
            }
            return keys;
        }
        return Collections.emptyList();
    }

    public List<String> getAllKeysByIds(Collection<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<String> keys = new ArrayList<>();
            for (String id : ids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, "*", id, "*")));
            }
            return keys;
        }
        return Collections.emptyList();
    }

    public List<DeviceCommand> getAll(final Boolean isUpdated) {
        final String isUpdatedKeyPart = isUpdated != null ? isUpdated.toString() : "*";
        Set<String> keys = redis.getAllKeys(String.format(KEY_FORMAT, "*", "*", isUpdatedKeyPart));
        List<DeviceCommand> commands = new ArrayList<>();
        for (final String key : keys) {
            final DeviceCommand command = getByKey(key);
            if (command != null) {
                commands.add(getByKey(key));
            }
        }
        return commands;
    }
}
