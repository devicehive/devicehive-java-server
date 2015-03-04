package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.Update;
import com.devicehive.messages.kafka.Command;
import com.devicehive.model.*;
import com.devicehive.model.enums.WorkerPath;
import com.devicehive.service.helpers.WorkerUtils;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.converters.DeviceCommandConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;


@Stateless
@LogExecutionTime
public class DeviceCommandService {
    private static final DeviceCommandConverter CONVERTER = new DeviceCommandConverter(null);
    private static final Random RANDOM = new Random();

    //TODO: Log more errors
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommand.class);

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
    private Event<DeviceCommandMessage> deviceCommandMessageReceivedEvent;

    @Inject
    @Command
    @Update
    private Event<DeviceCommandMessage> deviceCommandUpdateMessageReceivedEvent;


    public DeviceCommandMessage getByGuidAndId(List<String> deviceGuids, String commandId) {
        final List<DeviceCommandMessage> commands = getDeviceCommands(commandId, deviceGuids, null);
        if (!commands.isEmpty()) {
            return commands.get(0);
        } else {
            return null;
        }
    }

    public DeviceCommandMessage findById(String commandId) {
        final List<DeviceCommandMessage> commands = getDeviceCommands(commandId, null, null);
        if (!commands.isEmpty()) {
            return commands.get(0);
        } else {
            return null;
        }
    }

    public List<DeviceCommandMessage> getDeviceCommandsList(Collection<String> devices, Collection<String> names,
                                                     Timestamp timestamp,
                                                     HivePrincipal principal) {
        final String timestampStr = TimestampAdapter.formatTimestamp(timestamp);
        if (devices != null && !devices.isEmpty()) {
            return getDeviceCommands(null, deviceService.findGuidsWithPermissionsCheck(devices, principal), timestampStr);
        } else {
            return getDeviceCommands(null, null, timestampStr);
        }
    }

    public List<DeviceCommandMessage> queryDeviceCommand(final String deviceGuid, final String start, final String endTime, final String command,
                                                  final String status, final String sortField, final Boolean sortOrderAsc,
                                                  Integer take, Integer skip, Integer gridInterval) {
        final List<DeviceCommandMessage> commands = getDeviceCommands(null, Arrays.asList(deviceGuid), start);
        if (endTime != null) {
            final Timestamp end = TimestampQueryParamParser.parse(endTime);
            CollectionUtils.filter(commands, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return end.before(((DeviceCommandMessage) o).getTimestamp());
                }
            });
        }
        if (StringUtils.isNotBlank(status)) {
            CollectionUtils.filter(commands, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return status.equals(((DeviceCommandMessage) o).getStatus());
                }
            });
        }
        if (StringUtils.isNotBlank(command)) {
            CollectionUtils.filter(commands, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return command.equals(((DeviceCommandMessage) o).getCommand());
                }
            });
        }
        return commands;
    }

    public void submitDeviceCommandUpdate(DeviceCommandMessage message) {
        deviceCommandUpdateMessageReceivedEvent.fire(message);
    }

    public void submitDeviceCommand(DeviceCommandMessage message) {
        deviceCommandMessageReceivedEvent.fire(message);
    }

    public DeviceCommandMessage convertToDeviceCommandMessage(DeviceCommandWrapper command, Device device, User user, Long commandId) {

        DeviceCommandMessage message = new DeviceCommandMessage();
        if (commandId == null) {
            //TODO: Replace with UUID
            message.setId((long) Math.abs(RANDOM.nextInt()));
        } else {
            message.setId(commandId);
        }
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setCommand(command.getCommand());
        if (user != null) {
            message.setUserId(user.getId());
        }
        if (command.getParameters() != null) {
            message.setParameters(command.getParameters());
        }
        if (command.getLifetime() != null) {
            message.setLifetime(command.getLifetime());
        }
        if (command.getStatus() != null) {
            message.setStatus(command.getStatus());
        }
        if (command.getResult() != null) {
            message.setResult(command.getResult());
        }
        hiveValidator.validate(message);
        return message;
    }

    private List<DeviceCommandMessage> getDeviceCommands(String commandId, List<String> deviceGuids, String timestamp) {
        try {
            final JsonArray jsonArray = workerUtils.getDataFromWorker(commandId, deviceGuids, timestamp, WorkerPath.COMMANDS);
            List<DeviceCommandMessage> messages = new ArrayList<>();
            for (JsonElement command : jsonArray) {
                messages.add(CONVERTER.fromString(command.toString()));
            }
            return messages;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }
}
