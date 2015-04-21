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
public class RedisCommandService extends RedisService<DeviceCommand> {
    private static final String KEY_FORMAT = "command:%s:%s";

    @EJB
    private RedisConnector redis;
    @EJB
    private PropertiesService propertiesService;

    @Override
    @Asynchronous
    public void save(DeviceCommand deviceCommand) {
        final String key = String.format(KEY_FORMAT, deviceCommand.getDeviceGuid(), deviceCommand.getId());
        Map<String, String> commandMap = new HashMap<>();
        commandMap.put("id", deviceCommand.getId().toString());
        commandMap.put("deviceGuid", deviceCommand.getDeviceGuid());
        if (deviceCommand.getCommand() != null) {
            commandMap.put("command", deviceCommand.getCommand());
        }
        commandMap.put("parameters", (deviceCommand.getParameters() != null && deviceCommand.getParameters().getJsonString() != null)
                ? deviceCommand.getParameters().getJsonString() : "");
        commandMap.put("timestamp", deviceCommand.getTimestamp() != null ? TimestampAdapter.formatTimestamp(deviceCommand.getTimestamp()) : "");
        commandMap.put("userId", deviceCommand.getUserId() != null ? deviceCommand.getUserId().toString() : "");
        commandMap.put("lifetime", deviceCommand.getLifetime() != null ? deviceCommand.getLifetime().toString() : "");
        commandMap.put("flags", deviceCommand.getFlags() != null ? deviceCommand.getFlags().toString() : "");
        commandMap.put("result", (deviceCommand.getResult() != null && deviceCommand.getResult().getJsonString() != null)
                ? deviceCommand.getResult().getJsonString() : "");
        commandMap.put("status", deviceCommand.getStatus() != null ? deviceCommand.getStatus() : "");
        commandMap.put("isUpdated", deviceCommand.getIsUpdated() != null ? deviceCommand.getIsUpdated().toString() : "false");
        redis.setAll(key, commandMap, propertiesService.getProperty(Constants.COMMAND_EXPIRE_SEC));
    }

    @Override
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

    @Override
    public DeviceCommand getByIdAndGuid(final Long id, final String guid) {
        final String key = String.format(KEY_FORMAT, guid, id);
        return getByKey(key);
    }

    @Override
    public List<DeviceCommand> getByGuids(Collection<String> guids) {
        final List<String> keys = getAllKeysByGuids(guids);
        if (CollectionUtils.isNotEmpty(keys)) {
            List<DeviceCommand> commands = new ArrayList<>();
            for (final String key : keys) {
                commands.add(getByKey(key));
            }
            return commands;
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllKeysByGuids(Collection<String> guids) {
        if (CollectionUtils.isNotEmpty(guids)) {
            List<String> keys = new ArrayList<>();
            for (String guid : guids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, guid, "*")));
            }
            return keys;
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllKeysByIds(Collection<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<String> keys = new ArrayList<>();
            for (String id : ids) {
                keys.addAll(redis.getAllKeys(String.format(KEY_FORMAT, "*", id)));
            }
            return keys;
        }
        return Collections.emptyList();
    }

    @Override
    public List<DeviceCommand> getAll() {
        Set<String> keys = redis.getAllKeys(String.format(KEY_FORMAT, "*", "*"));
        List<DeviceCommand> commands = new ArrayList<>();
        for (final String key : keys) {
            commands.add(getByKey(key));
        }
        return commands;
    }
}
