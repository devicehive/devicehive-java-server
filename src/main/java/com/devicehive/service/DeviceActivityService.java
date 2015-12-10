package com.devicehive.service;

import com.devicehive.dao.DeviceDAO;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.devicehive.configuration.Constants.DEVICE_OFFLINE_STATUS;

@Component
@Lazy(false)
public class DeviceActivityService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceActivityService.class);
    private static final Integer PROCESS_DEVICES_BUFFER_SIZE = 100;
    private static final String DEVICE_ACTIVITY_MAP = "DEVICE-ACTIVITY";

    @Autowired
    private DeviceDAO deviceDAO;

    @Autowired
    private HazelcastInstance hzInstance;

    private IMap<String, Long> deviceActivityMap;


    @PostConstruct
    public void postConstruct() {
        deviceActivityMap = hzInstance.getMap(DEVICE_ACTIVITY_MAP);
    }

    public void update(String deviceGuid) {
        deviceActivityMap.put(deviceGuid, System.currentTimeMillis());
    }

    @Scheduled(cron = "0 * * * * *")//executing at start of every minute
    public void processOfflineDevices() {
        logger.debug("Checking lost offline devices");
        long now = System.currentTimeMillis();
        List<String> activityKeys = new ArrayList<>(deviceActivityMap.keySet());
        int indexFrom = 0;
        int indexTo = Math.min(activityKeys.size(), indexFrom + PROCESS_DEVICES_BUFFER_SIZE);
        while (indexFrom < indexTo) {
            List<String> guids = activityKeys.subList(indexFrom, indexTo);
            Map<String, Integer> devicesGuidsAndOfflineTime = deviceDAO.getDevicesGuidsAndOfflineTime(guids);
            doProcess(deviceActivityMap, guids, devicesGuidsAndOfflineTime, now);
            indexFrom = indexTo;
            indexTo = Math.min(activityKeys.size(), indexFrom + PROCESS_DEVICES_BUFFER_SIZE);
        }
        logger.debug("Checking lost offline devices complete");
    }

    private void doProcess(IMap<String, Long> fullDeviceActivityMap, List<String> guids, Map<String, Integer> devicesGuidsAndOfflineTime, Long now) {
        List<String> toUpdateStatus = new ArrayList<>();
        for (final String deviceGuid : guids) {
            if (!devicesGuidsAndOfflineTime.containsKey(deviceGuid)) {
                logger.warn("Device with guid {} does not exists", deviceGuid);
                fullDeviceActivityMap.remove(deviceGuid);
            } else {
                logger.debug("Checking device {} ", deviceGuid);
                Integer offlineTimeout = devicesGuidsAndOfflineTime.get(deviceGuid);
                if (offlineTimeout != null) {
                    Long time = fullDeviceActivityMap.get(deviceGuid);
                    if (now - time > offlineTimeout * 1000) {
                        if (fullDeviceActivityMap.remove(deviceGuid, time)) {
                            toUpdateStatus.add(deviceGuid);
                        }
                    }
                }
            }
        }
        if (!toUpdateStatus.isEmpty()) {
            deviceDAO.changeStatusForDevices(DEVICE_OFFLINE_STATUS, toUpdateStatus);
        }
    }

}
