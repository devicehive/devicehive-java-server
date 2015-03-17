package com.devicehive.service;

import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.messages.kafka.Notification;
import com.devicehive.model.*;
import com.devicehive.model.enums.WorkerPath;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
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
import java.sql.Timestamp;
import java.util.*;

@Stateless
@LogExecutionTime
public class DeviceNotificationService {
    // TODO: change to deviceGuid
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
    private Event<DeviceNotification> deviceNotificationMessageReceivedEvent;


    public List<DeviceNotification> getDeviceNotificationList(final Collection<String> deviceGuids, final String names,
                                                              final Timestamp timestamp) {
        final String timestampStr = TimestampAdapter.formatTimestamp(timestamp);
        return getDeviceNotifications(null, deviceGuids, names, timestampStr);

    }

    public DeviceNotification findById(final String notificationId) {
        final List<DeviceNotification> notifications = getDeviceNotifications(notificationId, null, null, null);
        if (!notifications.isEmpty()) {
            return notifications.get(0);
        } else {
            return null;
        }
    }

    public List<DeviceNotification> queryDeviceNotification(final String deviceGuid, final String start, final String endTime, final String notification,
                                                                   final String sortField, final Boolean sortOrderAsc,
                                                         Integer take, Integer skip, Integer gridInterval) {
        final List<DeviceNotification> notifications = getDeviceNotifications(null, Arrays.asList(deviceGuid), null, start);
        if (endTime != null) {
            final Timestamp end = TimestampQueryParamParser.parse(endTime);
            CollectionUtils.filter(notifications, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return end.before(((DeviceCommand) o).getTimestamp());
                }
            });
        }
        if (StringUtils.isNotBlank(notification)) {
            CollectionUtils.filter(notifications, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return notification.equals(((DeviceNotification) o).getNotification());
                }
            });
        }
        return notifications;
    }

    public void submitDeviceNotification(final DeviceNotification notification, final Device device) {
        List<DeviceNotification> proceedNotifications = processDeviceNotification(notification, device);
        for (DeviceNotification currentNotification : proceedNotifications) {
            deviceNotificationMessageReceivedEvent.fire(currentNotification);
        }
    }

    public void submitDeviceNotification(final DeviceNotification notification, final String deviceGuid) {
        notification.setId(UUIDs.timeBased().timestamp());
        notification.setDeviceGuid(deviceGuid);
        notification.setTimestamp(timestampService.getTimestamp());
        deviceNotificationMessageReceivedEvent.fire(notification);
    }

    public List<DeviceNotification> processDeviceNotification(DeviceNotification notificationMessage, Device device) {
        List<DeviceNotification> notificationsToCreate = new ArrayList<>();
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

    public DeviceNotification convertToMessage(DeviceNotificationWrapper notificationSubmit, Device device) {
        DeviceNotification message = new DeviceNotification();
        message.setId(UUIDs.timeBased().timestamp() + random.nextInt(1000));
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setNotification(notificationSubmit.getNotification());
        message.setParameters(notificationSubmit.getParameters());
        return message;
    }

    public DeviceNotification refreshDeviceStatusCase(DeviceNotification notificationMessage, Device device) {
        device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        device.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(device, SpecialNotifications.DEVICE_UPDATE);
    }

    private List<DeviceNotification> getDeviceNotifications(final String notificationId, final Collection<String> deviceGuids,
                                                            final String names, final String timestamp) {
        final JsonArray jsonArray = workerUtils.getDataFromWorker(notificationId, deviceGuids, names, timestamp, WorkerPath.NOTIFICATIONS);
        List<DeviceNotification> messages = new ArrayList<>();
        for (JsonElement command : jsonArray) {
            messages.add(CONVERTER.fromString(command.toString()));
        }
        return messages;
    }

}
