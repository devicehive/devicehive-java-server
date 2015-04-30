package com.devicehive.controllers;

import com.devicehive.domain.DeviceNotification;
import com.devicehive.exception.HiveException;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.devicehive.services.DeviceNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by tatyana on 2/11/15.
 */
@RestController
@RequestMapping("/notification")
public class DeviceNotificationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceNotificationController.class);

    @Autowired
    private DeviceNotificationService notificationService;
    @Autowired
    private TimestampAdapter timestampAdapter;

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public List<DeviceNotification> get(@RequestParam(value = "count", required=false, defaultValue = "100") int count,
                                          @RequestParam(value = "deviceGuids", required = false) String deviceGuids,
                                          @RequestParam(value = "names", required = false) String notificationNames,
                                          @RequestParam(value = "timestamp", required = false) String timestamp) {
        LOGGER.debug("/notifications list GET method requested with parameters: {}, {}, {}", deviceGuids, notificationNames, timestamp);
        final Timestamp date = timestampAdapter.parseTimestamp(timestamp);
        return notificationService.get(count, null, deviceGuids, notificationNames, date);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public DeviceNotification get(@PathVariable String id) {
        final List<DeviceNotification> notifications = notificationService.get(1, id, null, null, null);
        if (notifications.isEmpty()) {
            LOGGER.error("Notification with id {} not found", id);
            throw new HiveException("Notification not found", HttpStatus.NOT_FOUND);
        }
        return notifications.get(0);
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
