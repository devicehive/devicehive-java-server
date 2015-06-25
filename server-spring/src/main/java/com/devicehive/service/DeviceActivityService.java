package com.devicehive.service;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@Lazy(false)
public class DeviceActivityService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceActivityService.class);

    @Autowired
    private DeviceDAO deviceDAO;

    private Cache deviceActivityCache;

    @PostConstruct
    public void postConstruct() {
        deviceActivityCache = CacheManager.getInstance().getCache("deviceActivity");
    }

    public void update(String deviceGuid) {
        deviceActivityCache.put(new Element(deviceGuid, System.currentTimeMillis()));
    }

    @Scheduled(cron = "0 */5 * * * *")//(hour = "*", minute = "*/5", persistent = false)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
