package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.Update;
import com.devicehive.messages.kafka.Command;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.enums.WorkerPath;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.helpers.WorkerUtils;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import com.devicehive.websockets.converters.DeviceCommandConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


@Stateless
@LogExecutionTime
public class DeviceCommandService {
    private static final DeviceCommandConverter CONVERTER = new DeviceCommandConverter(null);
    private static final Random RANDOM = new Random();

    @EJB
    private DeviceService deviceService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private HiveValidator hiveValidator;
    @EJB
    private PropertiesService propertiesService;
    @EJB
    private WorkerUtils workerUtils;

    @Inject
    @Command
    @Create
    private Event<DeviceCommand> deviceCommandMessageReceivedEvent;

    @Inject
    @Command
    @Update
    private Event<DeviceCommand> deviceCommandUpdateMessageReceivedEvent;


    public DeviceCommand getByGuidAndId(List<String> deviceGuids, String commandId) {
        final List<DeviceCommand> commands = getDeviceCommands(commandId, deviceGuids, null, null);
        return !commands.isEmpty() ? commands.get(0) : null;
    }

    public DeviceCommand findById(String commandId) {
        final List<DeviceCommand> commands = getDeviceCommands(commandId, null, null, null);
        return !commands.isEmpty() ? commands.get(0) : null;
    }

    public List<DeviceCommand> getDeviceCommandsList(String devicesGuids, String commandNames,
                                                     Timestamp timestamp,
                                                     HivePrincipal principal) {
        final String timestampStr = TimestampAdapter.formatTimestamp(timestamp);
        if (StringUtils.isNotBlank(devicesGuids)) {
            return getDeviceCommands(null, deviceService.findGuidsWithPermissionsCheck(ParseUtil.getList(devicesGuids),
                    principal), commandNames, timestampStr);
        } else {
            return getDeviceCommands(null, null, commandNames, timestampStr);
        }
    }

    public List<DeviceCommand> queryDeviceCommand(final String deviceGuid, final String start, final String endTime, final String command,
                                                  final String status, final String sortField, final Boolean sortOrderAsc,
                                                  Integer take, Integer skip, Integer gridInterval) {
        final List<DeviceCommand> commands = getDeviceCommands(null, Arrays.asList(deviceGuid), null, start);
        if (endTime != null) {
            final Timestamp end = TimestampQueryParamParser.parse(endTime);
            CollectionUtils.filter(commands, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return end.before(((DeviceCommand) o).getTimestamp());
                }
            });
        }
        if (StringUtils.isNotBlank(status)) {
            CollectionUtils.filter(commands, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return status.equals(((DeviceCommand) o).getStatus());
                }
            });
        }
        if (StringUtils.isNotBlank(command)) {
            CollectionUtils.filter(commands, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return command.equals(((DeviceCommand) o).getCommand());
                }
            });
        }
        return commands;
    }

    public DeviceCommand convertToDeviceCommand(DeviceCommandWrapper commandWrapper, Device device, User user, Long commandId) {

        DeviceCommand command = new DeviceCommand();
        if (commandId == null) {
            //TODO: Replace with UUID
            command.setId((long) Math.abs(RANDOM.nextInt()));
        } else {
            command.setId(commandId);
        }
        command.setDeviceGuid(device.getGuid());
        command.setTimestamp(timestampService.getTimestamp());
        command.setCommand(commandWrapper.getCommand());
        if (user != null) {
            command.setUserId(user.getId());
        }
        if (commandWrapper.getParameters() != null) {
            command.setParameters(commandWrapper.getParameters());
        }
        if (commandWrapper.getLifetime() != null) {
            command.setLifetime(commandWrapper.getLifetime());
        }
        if (commandWrapper.getStatus() != null) {
            command.setStatus(commandWrapper.getStatus());
        }
        if (commandWrapper.getResult() != null) {
            command.setResult(commandWrapper.getResult());
        }
        hiveValidator.validate(command);
        return command;
    }

    private List<DeviceCommand> getDeviceCommands(final String commandId, final List<String> deviceGuids, final String names, final String timestamp) {
        final JsonArray jsonArray = workerUtils.getDataFromWorker(commandId, deviceGuids, names, timestamp, WorkerPath.COMMANDS);
        List<DeviceCommand> commands = new ArrayList<>();
        for (JsonElement command : jsonArray) {
            commands.add(CONVERTER.fromString(command.toString()));
        }
        return commands;
    }

    public void submitDeviceCommandUpdate(DeviceCommand message) {
        deviceCommandUpdateMessageReceivedEvent.fire(message);
    }

    public void submitDeviceCommand(DeviceCommand message) {
        deviceCommandMessageReceivedEvent.fire(message);
    }
}
