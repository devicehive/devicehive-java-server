package com.devicehive.service;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.GenericDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Component
@Lazy(false)
public class DeviceActivityService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceActivityService.class);

    private static final String DEVICE_ACTIVITY_MAP = "DEVICE-ACTIVITY";

    @Autowired
    private GenericDAO genericDAO;

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

    @Scheduled(cron = "0 */5 * * * *")//(hour = "*", minute = "*/5", persistent = false)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processOfflineDevices() {
        logger.debug("Checking lost offline devices");
        long now = System.currentTimeMillis();
        for (final String deviceGuid : deviceActivityMap.keySet()) {
            Device device = genericDAO.createNamedQuery(Device.class, "Device.findByUUID", Optional.of(CacheConfig.refresh()))
                    .setParameter("guid", deviceGuid)
                    .getResultList()
                    .stream().findFirst().orElse(null);
            if (device == null) {
                logger.warn("Device with guid {} does not exists", deviceGuid);
                deviceActivityMap.remove(deviceGuid);
            } else {
                logger.debug("Checking device {} ", device.getGuid());
                DeviceClass deviceClass = device.getDeviceClass();
                if (deviceClass.getOfflineTimeout() != null) {
                    Long time = deviceActivityMap.get(deviceGuid);
                    if (now - time > deviceClass.getOfflineTimeout() * 1000) {
                        if (deviceActivityMap.remove(deviceGuid, time)) {
                            Device device1 = genericDAO.createNamedQuery(Device.class, "Device.findByUUID", Optional.of(CacheConfig.refresh()))
                                    .setParameter("guid", deviceGuid)
                                    .getResultList()
                                    .stream().findFirst().orElse(null);
                            device1.setStatus("Offline");
                            logger.warn("Device {} is now offline", device1.getGuid());
                        }
                    }
                }
            }
        }
        logger.debug("Checking lost offline devices complete");
    }

}
