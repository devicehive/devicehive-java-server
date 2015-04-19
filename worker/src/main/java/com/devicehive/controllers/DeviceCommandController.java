package com.devicehive.controllers;

import com.devicehive.domain.DeviceCommand;
import com.devicehive.exception.HiveException;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.devicehive.services.DeviceCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * Created by tmatvienko on 2/13/15.
 */
@RestController
@RequestMapping("/command")
public class DeviceCommandController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandController.class);

    @Autowired
    private DeviceCommandService commandsService;
    @Autowired
    private TimestampAdapter timestampAdapter;

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public List<DeviceCommand> get(@RequestParam(value = "count", required=false, defaultValue = "100") int count,
                                            @RequestParam(value = "deviceGuids", required = false) final String deviceGuids,
                                            @RequestParam(value = "names", required = false) final String commandNames,
                                            @RequestParam(value = "timestamp", required = false) final String timestamp) {
        final Date date = timestampAdapter.parseDate(timestamp);
        return commandsService.get(count, null, deviceGuids, commandNames, date);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public DeviceCommand get(@PathVariable String id) {
        final List<DeviceCommand> commands = commandsService.get(1, id, null, null, null);
        if (commands.isEmpty()) {
            LOGGER.error("Command with id {} not found", id);
            throw new HiveException("Command not found", HttpStatus.NOT_FOUND);
        }
        return commands.get(0);
    }

    @RequestMapping(value="/count", method = RequestMethod.GET, produces = "application/json")
    public Long getCommandsCount() {
        return commandsService.getCommandsCount();
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public void deleteByDeviceGuid(@RequestParam(value = "deviceGuids", required = false) final String deviceGuids) {
        commandsService.delete(deviceGuids);
    }
}
