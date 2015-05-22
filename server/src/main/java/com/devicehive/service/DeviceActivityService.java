package com.devicehive.service;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.util.LogExecutionTime;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.List;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@EJB(beanInterface = DeviceActivityService.class, name = "DeviceActivityService")
@LogExecutionTime
public class DeviceActivityService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceActivityService.class);

    @EJB
    private DeviceDAO deviceDAO;

    private Cache deviceActivityCache;

    @PostConstruct
    public void postConstruct() {
        deviceActivityCache = CacheManager.getInstance().getCache("deviceActivity");
    }

    public void update(String deviceGuid) {
        deviceActivityCache.put(new Element(deviceGuid, System.currentTimeMillis()));
    }

    @Schedule(hour = "*", minute = "*/5", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void processOfflineDevices() {
        logger.debug("Checking lost offline devices");
        long now = System.currentTimeMillis();
        for (final String deviceGuid : (List<String>) deviceActivityCache.getKeys()) {
            Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceGuid);
            if (device == null) {
                logger.warn("Device with guid {} does not exists", deviceGuid);
                deviceActivityCache.remove(deviceGuid);
            } else {
                logger.debug("Checking device {} ", device.getGuid());
                DeviceClass deviceClass = device.getDeviceClass();
                if (deviceClass.getOfflineTimeout() != null) {
                    long time = (long) deviceActivityCache.get(deviceGuid).getObjectValue();
                    if (now - time > deviceClass.getOfflineTimeout() * 1000) {
                        if (deviceActivityCache.remove(deviceGuid)) {
                            deviceDAO.setOffline(deviceGuid);
                            logger.warn("Device {} is now offline", device.getGuid());
                        }
                    }
                }
            }
        }
        logger.debug("Checking lost offline devices complete");
    }

}
