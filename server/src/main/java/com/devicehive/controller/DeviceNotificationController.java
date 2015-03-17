package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.SortOrderQueryParamParser;
import com.devicehive.controller.converters.TimestampQueryParamParser;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.controller.util.SimpleWaiter;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.response.NotificationPollManyResponse;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.auth.AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION;
import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API:
 * DeviceNotification</a> for details.
 *
 * @author rroschin
 */
@Path("/device")
@LogExecutionTime
public class DeviceNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationController.class);
    @EJB
    private DeviceNotificationService notificationService;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceNotificationService deviceNotificationService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private TimestampService timestampService;
    @Resource(name = "concurrent/DeviceHiveWaitService")
    private ManagedExecutorService mes;
    @Inject
    private HiveSecurityContext hiveSecurityContext;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/query">DeviceHive
     * RESTful API: DeviceNotification: query</a> Queries device notifications.
     *
     * @param guid         Device unique identifier.
     * @param startTs      Filter by notification start timestamp (UTC).
     * @param endTs        Filter by notification end timestamp (UTC).
     * @param notification Filter by notification name.
     * @param sortField    Result list sort field. Available values are Timestamp (default) and Notification.
     * @param sortOrderSt  Result list sort order. Available values are ASC and DESC.
     * @param take         Number of records to take from the result list (default is 1000).
     * @param skip         Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceNotification">DeviceNotification</a> resources in the response body. <table>
     *         <tr> <td>Property Name</td> <td>Type</td> <td>Description</td> </tr> <tr> <td>id</td> <td>integer</td>
     *         <td>Notification identifier</td> </tr> <tr> <td>timestamp</td> <td>datetime</td> <td>Notification
     *         timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td> <td>Notification name</td> </tr>
     *         <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a JSON object with an arbitrary
     *         structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public Response query(@PathParam(DEVICE_GUID) String guid,
                          @QueryParam(START) String startTs,
                          @QueryParam(END) String endTs,
                          @QueryParam(NOTIFICATION) String notification,
                          @QueryParam(SORT_FIELD) @DefaultValue(TIMESTAMP) String sortField,
                          @QueryParam(SORT_ORDER) String sortOrderSt,
                          @QueryParam(TAKE) Integer take,
                          @QueryParam(SKIP) Integer skip,
                          @QueryParam(GRID_INTERVAL) Integer gridInterval) {

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        Timestamp start = TimestampQueryParamParser.parse(startTs);
        Timestamp end = TimestampQueryParamParser.parse(endTs);

        if (!TIMESTAMP.equalsIgnoreCase(sortField) && !NOTIFICATION.equalsIgnoreCase(sortField)) {
            logger.debug("Device notification query request failed Bad request sort field. Guid {}, start {}, end {}," +
                         " notification {}, sort field {}, sort order {}, take {}, skip {}", guid, start, end,
                         notification, sortField, sortOrder, take, skip);
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                                            new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                              Messages.INVALID_REQUEST_PARAMETERS));
        }
        sortField = sortField.toLowerCase();

        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        List<DeviceNotification> notifications =
                notificationService.queryDeviceNotification(device.getGuid(), startTs, endTs, notification, sortField, sortOrder, take,
                        skip, gridInterval);

        logger.debug("Device notification query succeed. Guid {}, start {}, end {}, notification {}, sort field {}," +
                     "sort order {}, take {}, skip {}", guid, start, end, notification, sortField, sortOrder, take,
                     skip);

        return ResponseFactory.response(Response.Status.OK, notifications, Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/get">DeviceHive RESTful
     * API: DeviceNotification: get</a> Gets information about device notification.
     *
     * @param guid           Device unique identifier.
     * @param notificationId Notification identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive .com/restful#Reference/DeviceNotification">DeviceNotification</a>
     *         resource in the response body. <table> <tr> <td>Property Name</td> <td>Type</td> <td>Description</td>
     *         </tr> <tr> <td>id</td> <td>integer</td> <td>Notification identifier</td> </tr> <tr> <td>timestamp</td>
     *         <td>datetime</td> <td>Notification timestamp (UTC)</td> </tr> <tr> <td>notification</td> <td>string</td>
     *         <td>Notification name</td> </tr> <tr> <td>parameters</td> <td>object</td> <td>Notification parameters, a
     *         JSON object with an arbitrary structure</td> </tr> </table>
     */
    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public Response get(@PathParam(DEVICE_GUID) String guid, @PathParam(ID) String notificationId) {
        logger.debug("Device notification requested. Guid {}, notification id {}", guid, notificationId);
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        DeviceNotification deviceNotification = notificationService.findById(notificationId);
        if (deviceNotification == null) {
            throw new HiveException(String.format(Messages.NOTIFICATION_NOT_FOUND, notificationId),
                                    NOT_FOUND.getStatusCode());
        }
        String deviceGuidFromNotification = deviceNotification.getDeviceGuid();
        if (!deviceGuidFromNotification.equals(guid)) {
            logger.debug("No device notifications found for device with guid : {}", guid);
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(
                                                String.format(Messages.NO_NOTIFICATIONS_FROM_DEVICE, guid)));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            logger.debug("No permissions to get notifications for device with guid : {}", guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                                            new ErrorResponse(
                                                String.format(Messages.NO_NOTIFICATIONS_FROM_DEVICE, guid)));
        }

        logger.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, deviceNotification, NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/poll">DeviceHive
     * RESTful API: DeviceNotification: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken
     *                   instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable
     *                   waiting.
     */
    @GET
    @Path("/{deviceGuid}/notification/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public void poll(
        @PathParam(DEVICE_GUID) final String deviceGuid,
        @QueryParam(NAMES) final String namesString,
        @QueryParam(TIMESTAMP) final String timestamp,
        @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
        @QueryParam(WAIT_TIMEOUT) final long timeout,
        @Suspended final AsyncResponse asyncResponse) {

        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse, false);
    }

    @GET
    @Path("/notification/poll")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public void pollMany(
        @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
        @QueryParam(WAIT_TIMEOUT) final long timeout,
        @QueryParam(DEVICE_GUIDS) String deviceGuidsString,
        @QueryParam(NAMES) final String namesString,
        @QueryParam(TIMESTAMP) final String timestamp,
        @Suspended final AsyncResponse asyncResponse) {

        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuidsString,
                      final String namesString,
                      final String timestampSt,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        final Timestamp timestamp = TimestampQueryParamParser.parse(timestampSt);

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
            }
        });

        mes.submit(new Runnable() {

            @Override
            public void run() {

                final List<String> devices = ParseUtil.getList(deviceGuidsString);

                try {
                    List<DeviceNotification> list =
                        getOrWaitForNotifications(principal, deviceGuidsString, namesString, timestamp, timeout);
                    Response response;
                    if (isMany) {
                        List<NotificationPollManyResponse> resultList = new ArrayList<>(list.size());
                        for (DeviceNotification notification : list) {
                            resultList.add(new NotificationPollManyResponse(notification,
                                                                            notification.getDeviceGuid()));
                        }
                        response =
                            ResponseFactory.response(Response.Status.OK, resultList, Policy.NOTIFICATION_TO_CLIENT);
                    } else {
                        response = ResponseFactory.response(Response.Status.OK, list, Policy.NOTIFICATION_TO_CLIENT);
                    }
                    asyncResponse.resume(response);
                } catch (Exception e) {
                    logger.error("Error: " + e.getMessage(), e);
                    asyncResponse.resume(e);
                }
            }
        });
    }

    private List<DeviceNotification> getOrWaitForNotifications(final HivePrincipal principal, final String deviceGuids,
                                                               final String names, Timestamp timestamp, long timeout) {
        logger.debug("Device notification pollMany requested for : {}, {}, {}.  Timeout = {}", deviceGuids, names,
                     timestamp, timeout);

        List<DeviceNotification> list = new ArrayList<>();
        final List<String> devices = ParseUtil.getList(deviceGuids);
        final List<String> availableDeviceGuids = StringUtils.isNotBlank(deviceGuids) ? deviceService.findGuidsWithPermissionsCheck(devices, principal) : Collections.EMPTY_LIST;
        if (timestamp != null) {
            list = deviceNotificationService.getDeviceNotificationList(availableDeviceGuids, names, timestamp);
        } else {
            timestamp = timestampService.getTimestamp();
        }

        if (list.isEmpty()) {
            NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
            UUID reqId = UUID.randomUUID();
            RestHandlerCreator<DeviceNotification> restHandlerCreator = new RestHandlerCreator<>();
            Set<NotificationSubscription> subscriptionSet = new HashSet<>();
            if (devices != null) {
                for (String guid : availableDeviceGuids) {
                    subscriptionSet
                        .add(new NotificationSubscription(principal, guid, reqId, names, restHandlerCreator));
                }
            } else {
                subscriptionSet
                    .add(new NotificationSubscription(principal, Constants.NULL_SUBSTITUTE,
                                                      reqId,
                                                      names,
                                                      restHandlerCreator));
            }

            if (SimpleWaiter
                .subscribeAndWait(storage, subscriptionSet, restHandlerCreator.getFutureTask(), timeout)) {
                list = deviceNotificationService.getDeviceNotificationList(devices, names, timestamp);
            }
            return list;
        }
        return list;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/insert">DeviceHive
     * RESTful API: DeviceNotification: insert</a> Creates new device notification.
     *
     * @param guid         Device unique identifier.
     * @param notificationSubmit In the request body, supply a DeviceNotification resource. <table> <tr> <td>Property
     *                     Name</td> <td>Required</td> <td>Type</td> <td>Description</td> </tr> <tr>
     *                     <td>notification</td> <td>Yes</td> <td>string</td> <td>Notification name.</td> </tr> <tr>
     *                     <td>parameters</td> <td>No</td> <td>object</td> <td>Notification parameters, a JSON object
     *                     with an arbitrary structure.</td> </tr> </table>
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     *         resource in the response body. <table> <tr> <tr>Property Name</tr> <tr>Type</tr> <tr>Description</tr>
     *         </tr> <tr> <td>id</td> <td>integer</td> <td>Notification identifier.</td> </tr> <tr> <td>timestamp</td>
     *         <td>datetime</td> <td>Notification timestamp (UTC).</td> </tr> </table>
     */
    @POST
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = CREATE_DEVICE_NOTIFICATION)
    public Response insert(@PathParam(DEVICE_GUID) String guid,
                           @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
                           DeviceNotificationWrapper notificationSubmit) {
        logger.debug("DeviceNotification insertAll requested");

        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        if (notificationSubmit == null || notificationSubmit.getNotification() == null){
            logger.debug(
                "DeviceNotification insertAll proceed with error. Bad notification: notification is required.");
            return ResponseFactory.response(BAD_REQUEST,
                                            new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                              Messages.INVALID_REQUEST_PARAMETERS));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        if (device.getNetwork() == null) {
            return ResponseFactory.response(FORBIDDEN,
                                            new ErrorResponse(FORBIDDEN.getStatusCode(),
                                                              String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK,
                                                                            guid)));
        }
        DeviceNotification message = notificationService.convertToMessage(notificationSubmit, device);
        notificationService.submitDeviceNotification(message, device);

        logger.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(CREATED, message, NOTIFICATION_TO_DEVICE);
    }

}