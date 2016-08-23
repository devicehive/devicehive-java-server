package com.devicehive.service;

import com.devicehive.dao.DeviceDao;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeviceNotificationService {

    @Autowired
    private DeviceEquipmentService deviceEquipmentService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private RpcClient rpcClient;

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private Gson gson;

    public Optional<DeviceNotification> find(Long id, String guid) {
        return hazelcastService.find(id, guid, DeviceNotification.class);
    }

    public Collection<DeviceNotification> find(Long id, String guid, Collection<String> devices,
                                               Collection<String> names,
                                               Date timestamp, Integer take) {
        return hazelcastService.find(id, guid, devices, names, timestamp, take, DeviceNotification.class);
    }

    public void submitDeviceNotification(final DeviceNotification notification, final DeviceVO device) {
        List<DeviceNotification> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotification currentNotification : proceedNotifications) {
            hazelcastService.store(currentNotification, DeviceNotification.class);
            rpcClient.push(Request.newBuilder()
//                    .withAction(Request.Action.NOTIFICATION_INSERT)
//                    .withBody(gson.toJson(notification).getBytes())
                    .withPartitionKey(device.getGuid())
                    .build());
        }
    }

    public void submitDeviceNotification(final DeviceNotification notification, final String deviceGuid) {
        notification.setTimestamp(timestampService.getTimestamp());
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceGuid(deviceGuid);
        hazelcastService.store(notification, DeviceNotification.class);
        rpcClient.push(Request.newBuilder()
//                .withAction(Request.Action.NOTIFICATION_INSERT)
//                .withBody(gson.toJson(notification).getBytes())
                .withPartitionKey(deviceGuid)
                .build());
    }

    public DeviceNotification convertToMessage(DeviceNotificationWrapper notificationSubmit, DeviceVO device) {
        DeviceNotification message = new DeviceNotification();
        message.setId(Math.abs(new Random().nextInt()));
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setNotification(notificationSubmit.getNotification());
        message.setParameters(notificationSubmit.getParameters());
        return message;
    }

    private List<DeviceNotification> processDeviceNotification(DeviceNotification notificationMessage, DeviceVO device) {
        List<DeviceNotification> notificationsToCreate = new ArrayList<>();
        switch (notificationMessage.getNotification()) {
            case SpecialNotifications.EQUIPMENT:
                deviceEquipmentService.refreshDeviceEquipment(notificationMessage, device);
                break;
            case SpecialNotifications.DEVICE_STATUS:
                DeviceNotification deviceNotification = refreshDeviceStatusCase(notificationMessage, device);
                notificationsToCreate.add(deviceNotification);
                break;
            default:
                break;

        }
        notificationsToCreate.add(notificationMessage);
        return notificationsToCreate;

    }

    private DeviceNotification refreshDeviceStatusCase(DeviceNotification notificationMessage, DeviceVO device) {
        DeviceVO devicevo = deviceDao.findByUUID(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        devicevo.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(devicevo, SpecialNotifications.DEVICE_UPDATE);
    }
}
