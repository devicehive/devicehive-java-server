package com.devicehive.resource.impl;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.resource.DeviceNotificationResource;
import com.devicehive.resource.converters.TimestampQueryParamParser;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.resource.util.SimpleWaiter;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ParseUtil;
import com.google.common.util.concurrent.Runnables;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Constants.DEFAULT_TAKE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceNotificationResourceImpl implements DeviceNotificationResource {
    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationResourceImpl.class);

    @Autowired
    private DeviceNotificationService notificationService;
    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService mes;

    /**
     * {@inheritDoc}
     */
    @Override
    public Response query(String guid, String startTs, String endTs, String notification, String sortField,
                          String sortOrderSt, Integer take, Integer skip, Integer gridInterval) {
        logger.debug("Device notification query requested for device {}", guid);
        Date timestamp = TimestampQueryParamParser.parse(startTs);

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        List<String> notificationNames = StringUtils.isNoneEmpty(notification) ? Collections.singletonList(notification) : null;

        final Collection<DeviceNotification> notificationList = notificationService.find(null, null, Arrays.asList(device.getGuid()),
                notificationNames, timestamp, take, principal);

        final Comparator<DeviceNotification> comparator = CommandResponseFilterAndSort.buildDeviceNotificationComparator(sortField);
        final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);

        final List<DeviceNotification> sortedDeviceNotifications = CommandResponseFilterAndSort.orderAndLimit(new ArrayList<>(notificationList),
                comparator, reverse, skip, take);

        logger.debug("Device notification query request proceed successfully for device {}", guid);
        return ResponseFactory.response(Response.Status.OK, sortedDeviceNotifications, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(String guid, Long notificationId) {
        logger.debug("Device notification requested. Guid {}, notification id {}", guid, notificationId);

        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        DeviceNotification notification = notificationService.find(notificationId, guid);

        if (notification == null) {
            logger.warn("Device notification get failed. NOT FOUND: No notification with id = {} found for device with guid = {}", notificationId, guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.NOTIFICATION_NOT_FOUND, notificationId)));
        }

        if (!notification.getDeviceGuid().equals(guid)) {
            logger.warn("Device notification get failed. BAD REQUEST: Notification with id = {} was not sent " +
                    "for device with guid = {}", notificationId, guid);
            return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    String.format(Messages.NOTIFICATION_NOT_FOUND, notificationId)));
        }

        logger.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, notification, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final String deviceGuid, final String namesString, final String timestamp, long timeout, final AsyncResponse asyncResponse) {
        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse, false);
    }

    @Override
    public void pollMany(long timeout, String deviceGuidsString, final String namesString, final String timestamp, final AsyncResponse asyncResponse) {
        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuidsString,
                      final String namesString,
                      final String timestamp,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        final Date ts = TimestampQueryParamParser.parse(timestamp);
        final String devices = StringUtils.isNoneBlank(deviceGuidsString) ? deviceGuidsString : null;
        final String names = StringUtils.isNoneBlank(namesString) ? namesString : null;

        mes.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    getOrWaitForNotifications(principal, devices, names, ts, timeout, asyncResponse, isMany);
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private void getOrWaitForNotifications(final HivePrincipal principal, final String devices,
                                                               final String names, final Date timestamp, long timeout,
                                                               final AsyncResponse asyncResponse, final boolean isMany) {
        logger.debug("Device notification pollMany requested for : {}, {}, {}.  Timeout = {}", devices, names, timestamp, timeout);
        if (timeout < 0) {
            submitEmptyResponse(asyncResponse);
        }

        final List<String> availableDevices = (StringUtils.isNotEmpty(devices))
                ? deviceService.findGuidsWithPermissionsCheck(ParseUtil.getList(devices), principal)
                : new ArrayList<>();
        final List<String> notificationNames = ParseUtil.getList(names);
        Collection<DeviceNotification> list = new ArrayList<>();
        NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
        final UUID reqId = UUID.randomUUID();
        Set<NotificationSubscription> subscriptionSet = new HashSet<>();
        FutureTask<Void> simpleWaitTask = new FutureTask<>(Runnables.doNothing(), null);

        if (!availableDevices.isEmpty()) {
            subscriptionSet.addAll(availableDevices.stream().map(guid ->
                    getNotificationInsertSubscription(principal, guid, reqId, names, asyncResponse, isMany, simpleWaitTask))
                    .collect(Collectors.toList()));
        } else {
            subscriptionSet.add(getNotificationInsertSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, names,
                    asyncResponse, isMany, simpleWaitTask));
        }
        
        if (timestamp != null && !availableDevices.isEmpty()) {
            list = notificationService.find(null, null, availableDevices, notificationNames, timestamp, DEFAULT_TAKE, principal);
        }

        if (!list.isEmpty()) {
            Response response = ResponseFactory.response(Response.Status.OK, list, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
            logger.debug("Notifications poll result: {}", response.getEntity());
            asyncResponse.resume(response);
        } else {
            if (!SimpleWaiter.subscribeAndWait(storage, subscriptionSet, simpleWaitTask, timeout)) {
                submitEmptyResponse(asyncResponse);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(String guid, DeviceNotificationWrapper notificationSubmit) {
        logger.debug("DeviceNotification insert requested: {}", notificationSubmit);

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (notificationSubmit == null || notificationSubmit.getNotification() == null) {
            logger.warn("DeviceNotification insert proceed with error. BAD REQUEST: notification is required.");
            ErrorResponse errorResponseEntity = new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    Messages.INVALID_REQUEST_PARAMETERS);
            return ResponseFactory.response(BAD_REQUEST, errorResponseEntity);
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            logger.warn("DeviceNotification insert proceed with error. NOT FOUND: device {} not found.", guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        if (device.getNetwork() == null) {
            logger.warn("DeviceNotification insert proceed with error. FORBIDDEN: Device {} is not connected to network.", guid);
            return ResponseFactory.response(FORBIDDEN, new ErrorResponse(FORBIDDEN.getStatusCode(),
                                                              String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, guid)));
        }
        DeviceNotification message = notificationService.convertToMessage(notificationSubmit, device);
        notificationService.submitDeviceNotification(message, device);

        logger.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(CREATED, message, NOTIFICATION_TO_DEVICE);
    }

    private void submitEmptyResponse(final AsyncResponse asyncResponse) {
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(),
                JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
    }

    private NotificationSubscription getNotificationInsertSubscription(HivePrincipal principal, String guid, UUID reqId, String names,
                          AsyncResponse asyncResponse, boolean isMany, FutureTask<Void> waitTask){
        return new NotificationSubscription(principal, guid, reqId, names,
                RestHandlerCreator.createNotificationInsert(asyncResponse, isMany, waitTask));
    }
}
