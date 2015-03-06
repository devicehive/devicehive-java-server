package com.devicehive.controller;

import com.devicehive.domain.wrappers.DeviceNotificationWrapper;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.devicehive.service.DeviceNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by tatyana on 2/11/15.
 */
@RestController
@RequestMapping("/notifications")
public class DeviceNotificationController {

    @Autowired
    private DeviceNotificationService notificationService;
    @Autowired
    private TimestampAdapter timestampAdapter;

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public List<DeviceNotificationWrapper> get(@RequestParam(value = "count", required=false, defaultValue = "1000") int count,
                                          @RequestParam(value = "id", required = false) final String id,
                                          @RequestParam(value = "deviceGuids", required = false) String deviceGuids,
                                          @RequestParam(value = "names", required = false) String notificationNames,
                                          @RequestParam(value = "timestamp", required = false) String timestamp) {
        final Timestamp date = timestampAdapter.parseTimestamp(timestamp);
        return notificationService.get(count, id, deviceGuids, notificationNames, date);
    }

    @RequestMapping(value="/count", method = RequestMethod.GET, produces = "application/json")
    public Long getCommandsCount() {
        return notificationService.getNotificationsCount();
    }

    @RequestMapping(value="/{deviceGuid}", method = RequestMethod.DELETE, produces = "application/json")
    public void deleteByDeviceGuid(@PathVariable String deviceGuid) {
        notificationService.delete(deviceGuid);
    }
}
