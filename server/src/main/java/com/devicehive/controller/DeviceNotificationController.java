package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.controller.util.SimpleWaiter;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.response.NotificationPollManyResponse;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.AsynchronousExecutor;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.devicehive.auth.AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION;
import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API: DeviceNotification</a> for details.
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

    @EJB
    private AsynchronousExecutor executor;


    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/query">DeviceHive
     * RESTful API: DeviceNotification: query</a>
     * Queries device notifications.
     *
     * @param guid         Device unique identifier.
     * @param start        Filter by notification start timestamp (UTC).
     * @param end          Filter by notification end timestamp (UTC).
     * @param notification Filter by notification name.
     * @param sortField    Result list sort field. Available values are Timestamp (default) and Notification.
     * @param sortOrder    Result list sort order. Available values are ASC and DESC.
     * @param take         Number of records to take from the result list (default is 1000).
     * @param skip         Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceNotification">DeviceNotification</a> resources in the response body.
     *         <table>
     *         <tr>
     *         <td>Property Name</td>
     *         <td>Type</td>
     *         <td>Description</td>
     *         </tr>
     *         <tr>
     *         <td>id</td>
     *         <td>integer</td>
     *         <td>Notification identifier</td>
     *         </tr>
     *         <tr>
     *         <td>timestamp</td>
     *         <td>datetime</td>
     *         <td>Notification timestamp (UTC)</td>
     *         </tr>
     *         <tr>
     *         <td>notification</td>
     *         <td>string</td>
     *         <td>Notification name</td>
     *         </tr>
     *         <tr>
     *         <td>parameters</td>
     *         <td>object</td>
     *         <td>Notification parameters, a JSON object with an arbitrary structure</td>
     *         </tr>
     *         </table>
     */
    @GET
    @Path("/{deviceGuid}/notification")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
    public void query(@PathParam(DEVICE_GUID) final String guid,
                          @QueryParam(START) final Timestamp start,
                          @QueryParam(END) final Timestamp end,
                          @QueryParam(NOTIFICATION) final String notification,
                          @QueryParam(SORT_FIELD) @DefaultValue(TIMESTAMP) final String sortField,
                          @QueryParam(SORT_ORDER) @SortOrder final Boolean sortOrder,
                          @QueryParam(TAKE) final Integer take,
                          @QueryParam(SKIP) final Integer skip,
                          @QueryParam(GRID_INTERVAL) final Integer gridInterval,
                          @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device notification query requested. Guid {}, start {}, end {}, notification {}, sort field {}," +
                "sort order {}, take {}, skip {}", guid, start, end, notification, sortField, sortOrder, take, skip);

        final boolean sortOrderRes = sortOrder == null ? true : sortOrder;
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("Device notification query succeed. Guid {}, start {}, end {}, notification {}, sort field {}," +
                        "sort order {}, take {}, skip {}", guid, start, end, notification, sortField, sortOrder, take, skip);
            }
        });
        final HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!TIMESTAMP.equalsIgnoreCase(sortField) && !NOTIFICATION.equalsIgnoreCase(sortField)) {
                    logger.debug("Device notification query request failed Bad request sort field. Guid {}, start {}, end {}," +
                            " notification {}, sort field {}, sort order {}, take {}, skip {}", guid, start, end,
                            notification, sortField, sortOrder, take, skip);
                    asyncResponse.resume(ResponseFactory.response(Response.Status.BAD_REQUEST,
                            new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_REQUEST_PARAMETERS)));
                }
                final String sortFieldLower = sortField.toLowerCase();


                Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

                List<DeviceNotification> result = notificationService.queryDeviceNotification(device, start, end,
                        notification, sortFieldLower, sortOrderRes, take, skip, gridInterval);
                asyncResponse.resume(ResponseFactory.response(Response.Status.OK, result,
                        Policy.NOTIFICATION_TO_CLIENT));
            }
        });

    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/get">DeviceHive
     * RESTful API: DeviceNotification: get</a>
     * Gets information about device notification.
     *
     * @param guid           Device unique identifier.
     * @param notificationId Notification identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceNotification">DeviceNotification</a> resource in the response body.
     *         <table>
     *         <tr>
     *         <td>Property Name</td>
     *         <td>Type</td>
     *         <td>Description</td>
     *         </tr>
     *         <tr>
     *         <td>id</td>
     *         <td>integer</td>
     *         <td>Notification identifier</td>
     *         </tr>
     *         <tr>
     *         <td>timestamp</td>
     *         <td>datetime</td>
     *         <td>Notification timestamp (UTC)</td>
     *         </tr>
     *         <tr>
     *         <td>notification</td>
     *         <td>string</td>
     *         <td>Notification name</td>
     *         </tr>
     *         <tr>
     *         <td>parameters</td>
     *         <td>object</td>
     *         <td>Notification parameters, a JSON object with an arbitrary structure</td>
     *         </tr>
     *         </table>
     */
    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    public Response get(@PathParam(DEVICE_GUID) String guid, @PathParam(ID) Long notificationId) {
        logger.debug("Device notification requested. Guid {}, notification id {}", guid, notificationId);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        DeviceNotification deviceNotification = notificationService.findById(notificationId);
        if (deviceNotification == null) {
            throw new HiveException(String.format(Messages.NOTIFICATION_NOT_FOUND, notificationId),
                    NOT_FOUND.getStatusCode());
        }
        String deviceGuidFromNotification = deviceNotification.getDevice().getGuid();
        if (!deviceGuidFromNotification.equals(guid)) {
            logger.debug("No device notifications found for device with guid : {}", guid);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(String.format(Messages.NO_NOTIFICATIONS_FROM_DEVICE, guid)));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            logger.debug("No permissions to get notifications for device with guid : {}", guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse(String.format(Messages.NO_NOTIFICATIONS_FROM_DEVICE, guid)));
        }

        logger.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, deviceNotification, NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/poll">DeviceHive RESTful API: DeviceNotification: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
    @Path("/{deviceGuid}/notification/poll")
    public void poll(
            @PathParam(DEVICE_GUID) final String deviceGuid,
            @QueryParam(NAMES) final String namesString,
            @QueryParam(TIMESTAMP) final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) final long timeout,
            @Suspended final AsyncResponse asyncResponse) {

        poll(timeout, deviceGuid, namesString, timestamp, asyncResponse, false);
    }

    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @Path("/notification/poll")
    public void pollMany(
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam(WAIT_TIMEOUT) final long timeout,
            @QueryParam(DEVICE_GUIDS) String deviceGuidsString,
            @QueryParam(NAMES) final String namesString,
            @QueryParam(TIMESTAMP) final Timestamp timestamp,
            @Suspended final AsyncResponse asyncResponse) {

        poll(timeout, deviceGuidsString, namesString, timestamp, asyncResponse, true);
    }

    private void poll(final long timeout,
                      final String deviceGuidsString,
                      final String namesString,
                      final Timestamp timestamp,
                      final AsyncResponse asyncResponse,
                      final boolean isMany) {
        final HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug(
                        "Device notification poll many proceed successfully for devices: {} with names, timestamp {}",
                        deviceGuidsString, namesString, timestamp);
            }
        });

        executor.execute(new Runnable() {


            @Override
            public void run() {

                final List<String> devices = ParseUtil.getList(deviceGuidsString);
                final List<String> names = ParseUtil.getList(namesString);

                try {
                    List<DeviceNotification> list =
                            getOrWaitForNotifications(principal, devices, names, timestamp, timeout);
                    Response response;
                    if (isMany) {
                        List<NotificationPollManyResponse> resultList = new ArrayList<>(list.size());
                        for (DeviceNotification notification : list) {
                            resultList.add(new NotificationPollManyResponse(notification,
                                    notification.getDevice().getGuid()));
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

    private List<DeviceNotification> getOrWaitForNotifications(HivePrincipal principal,
                                                               List<String> devices,
                                                               List<String> names,
                                                               Timestamp timestamp,
                                                               long timeout) {
        logger.debug("Device notification pollMany requested for : {}, {}, {}.  Timeout = {}", devices, names,
                timestamp, timeout);


        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        List<DeviceNotification> list =
                deviceNotificationService.getDeviceNotificationList(devices, names, timestamp, principal);

        if (list.isEmpty()) {
            NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
            UUID reqId = UUID.randomUUID();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            Set<NotificationSubscription> subscriptionSet = new HashSet<>();
            if (devices != null) {
                List<Device> actualDevices = deviceService.findByGuidWithPermissionsCheck(devices, principal);
                for (Device d : actualDevices) {
                    subscriptionSet
                            .add(new NotificationSubscription(principal, d.getId(), reqId, names, restHandlerCreator));
                }
            } else {
                subscriptionSet
                        .add(new NotificationSubscription(principal, Constants.NULL_ID_SUBSTITUTE,
                                reqId,
                                names,
                                restHandlerCreator));
            }

            if (SimpleWaiter
                    .subscribeAndWait(storage, subscriptionSet, restHandlerCreator.getFutureTask(), timeout)) {
                list = deviceNotificationService.getDeviceNotificationList(devices, names, timestamp, principal);
            }
            return list;
        }
        return list;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/insert">DeviceHive
     * RESTful API: DeviceNotification: insert</a>
     * Creates new device notification.
     *
     * @param guid         Device unique identifier.
     * @param notification In the request body, supply a DeviceNotification resource.
     *                     <table>
     *                     <tr>
     *                     <td>Property Name</td>
     *                     <td>Required</td>
     *                     <td>Type</td>
     *                     <td>Description</td>
     *                     </tr>
     *                     <tr>
     *                     <td>notification</td>
     *                     <td>Yes</td>
     *                     <td>string</td>
     *                     <td>Notification name.</td>
     *                     </tr>
     *                     <tr>
     *                     <td>parameters</td>
     *                     <td>No</td>
     *                     <td>object</td>
     *                     <td>Notification parameters, a JSON object with an arbitrary structure.</td>
     *                     </tr>
     *                     </table>
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a> resource in the response body.
     *         <table>
     *         <tr>
     *         <tr>Property Name</tr>
     *         <tr>Type</tr>
     *         <tr>Description</tr>
     *         </tr>
     *         <tr>
     *         <td>id</td>
     *         <td>integer</td>
     *         <td>Notification identifier.</td>
     *         </tr>
     *         <tr>
     *         <td>timestamp</td>
     *         <td>datetime</td>
     *         <td>Notification timestamp (UTC).</td>
     *         </tr>
     *         </table>
     */
    @POST
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = {CREATE_DEVICE_NOTIFICATION})
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam(DEVICE_GUID) String guid,
                           @JsonPolicyDef(NOTIFICATION_FROM_DEVICE) DeviceNotification notification) {
        logger.debug("DeviceNotification insertAll requested");
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (notification == null || notification.getNotification() == null) {
            logger.debug(
                    "DeviceNotification insertAll proceed with error. Bad notification: notification is required.");
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_REQUEST_PARAMETERS));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        if (device.getNetwork() == null) {
            return ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(FORBIDDEN.getStatusCode(),
                            String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, guid)));
        }
        notificationService.submitDeviceNotification(notification, device);

        logger.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(CREATED, notification, NOTIFICATION_TO_DEVICE);
    }


}