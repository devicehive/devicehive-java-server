package com.devicehive.service;

import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.messages.kafka.Notification;
import com.devicehive.model.*;
import com.devicehive.model.enums.WorkerPath;
import com.devicehive.service.helpers.WorkerUtils;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.converters.DeviceNotificationConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

@Stateless
@LogExecutionTime
public class DeviceNotificationService {
    private static final Random random = new Random();
    private static final DeviceNotificationConverter CONVERTER = new DeviceNotificationConverter(null);

    @EJB
    private DeviceEquipmentService deviceEquipmentService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private DeviceService deviceService;
    @EJB
    private WorkerUtils workerUtils;

    @Inject
    @Notification
    private Event<DeviceNotificationMessage> deviceNotificationMessageReceivedEvent;


    public List<DeviceNotificationMessage> getDeviceNotificationList(List<String> deviceGuids, Collection<String> names,
                                                              Timestamp timestamp) {
        final String timestampStr = TimestampAdapter.formatTimestamp(timestamp);
        return getDeviceNotifications(null, deviceGuids, timestampStr);

    }

    public DeviceNotificationMessage findById(String notificationId) {
        final List<DeviceNotificationMessage> notifications = getDeviceNotifications(notificationId, null, null);
        if (!notifications.isEmpty()) {
            return notifications.get(0);
        } else {
            return null;
        }
    }

    public List<DeviceNotificationMessage> queryDeviceNotification(final String deviceGuid, final String start, final String endTime, final String notification,
                                                                   final String sortField, final Boolean sortOrderAsc,
                                                         Integer take, Integer skip, Integer gridInterval) {
        final List<DeviceNotificationMessage> notifications = getDeviceNotifications(null, Arrays.asList(deviceGuid), start);
        if (endTime != null) {
            final Timestamp end = TimestampQueryParamParser.parse(endTime);
            CollectionUtils.filter(notifications, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return end.before(((DeviceCommandMessage) o).getTimestamp());
                }
            });
        }
        if (StringUtils.isNotBlank(notification)) {
            CollectionUtils.filter(notifications, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return notification.equals(((DeviceNotificationMessage) o).getNotification());
                }
            });
        }
        return notifications;
    }

    public void submitDeviceNotification(DeviceNotificationMessage notification, Device device) {
        List<DeviceNotificationMessage> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotificationMessage currentNotification : proceedNotifications) {
            deviceNotificationMessageReceivedEvent.fire(currentNotification);
        }
    }

    public void submitDeviceNotification(DeviceNotificationMessage notification, String deviceGuid) {
        notification.setId(UUIDs.timeBased().timestamp());
        notification.setDeviceGuid(deviceGuid);
        notification.setTimestamp(timestampService.getTimestamp());
        deviceNotificationMessageReceivedEvent.fire(notification);
    }

    public List<DeviceNotificationMessage> processDeviceNotification(DeviceNotificationMessage notificationMessage, Device device) {
        List<DeviceNotificationMessage> notificationsToCreate = new ArrayList<>();
        switch (notificationMessage.getNotification()) {
            case SpecialNotifications.EQUIPMENT:
                deviceEquipmentService.refreshDeviceEquipment(notificationMessage, device);
                break;
            case SpecialNotifications.DEVICE_STATUS:
                notificationsToCreate.add(refreshDeviceStatusCase(notificationMessage, device));
                break;
            default:
                break;

        }
        notificationMessage.setDeviceGuid(device.getGuid());
        notificationMessage.setTimestamp(timestampService.getTimestamp());
        notificationsToCreate.add(notificationMessage);
        return notificationsToCreate;

    }

    public DeviceNotificationMessage convertToMessage(DeviceNotificationWrapper notificationSubmit, Device device) {
        DeviceNotificationMessage message = new DeviceNotificationMessage();
        message.setId(UUIDs.timeBased().timestamp() + random.nextInt(1000));
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setNotification(notificationSubmit.getNotification());
        message.setParameters(notificationSubmit.getParameters());
        return message;
    }

    public DeviceNotificationMessage refreshDeviceStatusCase(DeviceNotificationMessage notificationMessage, Device device) {
        device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        device.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_UPDATE);
    }

    private List<DeviceNotificationMessage> getDeviceNotifications(String notificationId, List<String> deviceGuids, String timestamp) {
        try {
            final JsonArray jsonArray = workerUtils.getDataFromWorker(notificationId, deviceGuids, timestamp, WorkerPath.NOTIFICATIONS);
            List<DeviceNotificationMessage> messages = new ArrayList<>();
            for (JsonElement command : jsonArray) {
                messages.add(CONVERTER.fromString(command.toString()));
            }
            return messages;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

}
