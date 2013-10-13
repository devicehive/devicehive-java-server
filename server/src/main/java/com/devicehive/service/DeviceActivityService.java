package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.util.LogExecutionTime;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@EJB(beanInterface = DeviceActivityService.class, name = "DeviceActivityService")
@LogExecutionTime
public class DeviceActivityService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceActivityService.class);
    @EJB
    private HazelcastService hazelcastService;
    @EJB
    private DeviceDAO deviceDAO;
    private HazelcastInstance hazelcast;
    private IMap<Long, Long> deviceTimestampMap;

    @PostConstruct
    public void postConstruct() {
        hazelcast = hazelcastService.getHazelcast();
        deviceTimestampMap = hazelcast.getMap(Constants.DEVICE_ACTIVITY_MAP);
    }

    public void update(long deviceId) {
        deviceTimestampMap.putAsync(deviceId, hazelcast.getCluster().getClusterTime());
    }

    @Schedule(hour = "*", minute = "*/1", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void processOfflineDevices() {
        logger.debug("Checking lost offline devices");
        long now = hazelcast.getCluster().getClusterTime();
        for (Long deviceId : deviceTimestampMap.localKeySet()) {
            Device device = deviceDAO.findById(deviceId);
            if (device == null) {
                logger.warn("Device with id {} does not exists", deviceId);
                deviceTimestampMap.remove(deviceId);
            } else {
                logger.debug("Checking device {} ", device.getGuid());
                DeviceClass deviceClass = device.getDeviceClass();
                if (deviceClass.getOfflineTimeout() != null) {
                    long time = deviceTimestampMap.get(deviceId);
                    if (now - time > deviceClass.getOfflineTimeout() * 1000) {
                        if (deviceTimestampMap.remove(deviceId, time)) {
                            deviceDAO.setOffline(deviceId);
                            logger.warn("Device {} is now offline", device.getGuid());
                        }
                    }
                }
            }
        }
        logger.debug("Checking lost offline devices complete");
    }

}
