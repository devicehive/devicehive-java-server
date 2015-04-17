package com.devicehive.controllers;

import com.devicehive.domain.DeviceCommand;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.devicehive.services.DeviceCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.PathParam;
import java.util.Date;
import java.util.List;

/**
 * Created by tmatvienko on 2/13/15.
 */
@RestController
@RequestMapping("/command")
public class DeviceCommandController {

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
    public List<DeviceCommand> get(@PathParam(value = "id") final String commandId) {
        return commandsService.get(null, commandId, null, null, null);
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
