package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.utils.LogExecutionTime;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private IMap<Long,Long> deviceTimestampMap;


    @PostConstruct
    public void postConstruct() {
        hazelcast = hazelcastService.getHazelcast();
        deviceTimestampMap = hazelcast.getMap(Constants.DEVICE_ACTIVITY_MAP);
    }


    public void update(long deviceId) {
        deviceTimestampMap.putAsync(deviceId, hazelcast.getCluster().getClusterTime());
    }




    @Schedule(hour = "*", minute = "*/1")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void processOfflineDevices() {
        long now = hazelcast.getCluster().getClusterTime();
        for (Iterator<Long> iter = deviceTimestampMap.localKeySet().iterator(); iter.hasNext();) {
            Long deviceId = iter.next();
            Device device = deviceDAO.findById(deviceId);
            logger.debug("Checking device {} ", device.getGuid());
            DeviceClass deviceClass = device.getDeviceClass();
            if (deviceClass.getOfflineTimeout() != null) {
                if (now - deviceTimestampMap.get(deviceId) > deviceClass.getOfflineTimeout() * 1000) {
                    deviceDAO.setOffline(deviceId);
                    iter.remove();
                    logger.warn("Device {} is now offline", device.getGuid());
                }
            }
        }
    }

}
