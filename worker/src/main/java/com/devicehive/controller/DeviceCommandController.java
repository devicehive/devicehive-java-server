package com.devicehive.controller;

import com.devicehive.domain.wrappers.DeviceCommandWrapper;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.devicehive.service.DeviceCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by tmatvienko on 2/13/15.
 */
@RestController
@RequestMapping("/commands")
public class DeviceCommandController {

    @Autowired
    private DeviceCommandService commandsService;
    @Autowired
    private TimestampAdapter timestampAdapter;

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public List<DeviceCommandWrapper> get(@RequestParam(value = "count", required=false, defaultValue = "1000") int count,
                                            @RequestParam(value = "id", required = false) final String id,
                                            @RequestParam(value = "deviceGuids", required = false) final String deviceGuids,
                                            @RequestParam(value = "names", required = false) final String commandNames,
                                            @RequestParam(value = "timestamp", required = false) final String timestamp) {
        final Timestamp date = timestampAdapter.parseTimestamp(timestamp);
        return commandsService.get(count, id, deviceGuids, commandNames, date);
    }

    @RequestMapping(value="/count", method = RequestMethod.GET, produces = "application/json")
    public Long getCommandsCount() {
        return commandsService.getCommandsCount();
    }

    @RequestMapping(value="/{deviceGuid}", method = RequestMethod.DELETE, produces = "application/json")
    public void deleteByDeviceGuid(@PathVariable final String deviceGuid) {
        commandsService.delete(deviceGuid);
    }
}
