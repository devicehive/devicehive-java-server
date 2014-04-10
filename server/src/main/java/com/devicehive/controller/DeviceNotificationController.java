package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
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
import com.devicehive.model.*;
import com.devicehive.model.response.NotificationPollManyResponse;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ParseUtil;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Singleton;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.devicehive.auth.AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API: DeviceNotification</a> for details.
 *
 * @author rroschin
 */
@Path("/device")
@LogExecutionTime
@Singleton
public class DeviceNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationController.class);
    private DeviceNotificationService notificationService;
    private SubscriptionManager subscriptionManager;
    private DeviceNotificationService deviceNotificationService;
    private DeviceService deviceService;
    private TimestampService timestampService;
    private ExecutorService asyncPool;

    @EJB
    public void setNotificationService(DeviceNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EJB
    public void setSubscriptionManager(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @EJB
    public void setDeviceNotificationService(DeviceNotificationService deviceNotificationService) {
        this.deviceNotificationService = deviceNotificationService;
    }

    @EJB
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @EJB
    public void setTimestampService(TimestampService timestampService) {
        this.timestampService = timestampService;
    }

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
    public Response query(@PathParam("deviceGuid") String guid,
                          @QueryParam("start") Timestamp start,
                          @QueryParam("end") Timestamp end,
                          @QueryParam("notification") String notification,
                          @QueryParam("sortField") String sortField,
                          @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                          @QueryParam("take") Integer take,
                          @QueryParam("skip") Integer skip,
                          @QueryParam("gridInterval") Integer gridInterval) {

        logger.debug("Device notification query requested. Guid {}, start {}, end {}, notification {}, sort field {}," +
                "sort order {}, take {}, skip {}", guid, start, end, notification, sortField, sortOrder, take, skip);

        if (sortOrder == null) {
            sortOrder = true;
        }

        if (!"Timestamp".equals(sortField) && !"Notification".equals(sortField) && sortField != null) {
            logger.debug("Device notification query request failed Bad request sort field. Guid {}, start {}, end {}," +
                    " notification {}, sort field {}, sort order {}, take {}, skip {}", guid, start, end,
                    notification, sortField, sortOrder, take, skip);
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        } else if (sortField != null) {
            sortField = StringUtils.uncapitalize(sortField);
        }

        if (sortField == null) {
            sortField = "timestamp";
        }

        sortField = sortField.toLowerCase();

        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        List<DeviceNotification> result = notificationService.queryDeviceNotification(device, start, end,
                notification, sortField, sortOrder, take, skip, gridInterval);

        logger.debug("Device notification query succeed. Guid {}, start {}, end {}, notification {}, sort field {}," +
                "sort order {}, take {}, skip {}", guid, start, end, notification, sortField, sortOrder, take, skip);

        return ResponseFactory.response(Response.Status.OK, result, Policy.NOTIFICATION_TO_CLIENT);
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
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") Long notificationId) {
        logger.debug("Device notification requested. Guid {}, notification id {}", guid, notificationId);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        DeviceNotification deviceNotification = notificationService.findById(notificationId);
        if (deviceNotification == null) {
            throw new HiveException("Device notification with id : " + notificationId + " not found",
                    NOT_FOUND.getStatusCode());
        }
        String deviceGuidFromNotification = deviceNotification.getDevice().getGuid();
        if (!deviceGuidFromNotification.equals(guid)) {
            logger.debug("No device notifications found for device with guid : {}", guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse("No device notifications " +
                    "found for device with guid : " + guid));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            logger.debug("No permissions to get notifications for device with guid : {}", guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("No device notifications " +
                    "found for device with guid : " + guid));
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
            @PathParam("deviceGuid") final String deviceGuid,
            @QueryParam("names") final String namesString,
            @QueryParam("timestamp") final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT) @QueryParam
                    ("waitTimeout") final long timeout,
            @Suspended final AsyncResponse asyncResponse) {

        SubscriptionFilterInternal subscriptionFilter = SubscriptionFilterInternal
                .createForSingleDevice(deviceGuid, ParseUtil.getList(namesString), timestamp);
        pollMany(timeout, subscriptionFilter, asyncResponse);
    }


    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @Path("/notification/poll")
    @Deprecated
    public void pollMany(
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @QueryParam("timestamp") final Timestamp timestamp,
            @QueryParam("deviceGuids") final String deviceGuids,
            @Suspended final AsyncResponse asyncResponse) {
        SubscriptionFilterInternal subscriptionFilter =
                SubscriptionFilterInternal.createForManyDevices(ParseUtil.getList(deviceGuids), timestamp);
        pollMany(timeout, subscriptionFilter, asyncResponse);
    }     
    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/pollMany">DeviceHive RESTful API: DeviceNotification: pollMany</a>
     *
     * @param external Device unique identifiers with names and
     *                          timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param timeout           Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     */

    @POST
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @Path("/notification/poll")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void pollManyPost(
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            final SubscriptionFilterExternal external,
            @Suspended final AsyncResponse asyncResponse) {
        SubscriptionFilterInternal subscriptionFilter = SubscriptionFilterInternal.create(external);
        pollMany(timeout, subscriptionFilter, asyncResponse);
    }


    private void pollMany(final long timeout,
                          final SubscriptionFilterInternal subscriptionFilter,
                          final AsyncResponse asyncResponse) {
        final HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("Device notification poll many proceed successfully for devices: {}", subscriptionFilter);
            }
        });

        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<DeviceNotification> list =
                            getOrWaitForNotifications(principal, subscriptionFilter, timeout);
                    List<NotificationPollManyResponse> resultList = new ArrayList<>(list.size());
                    for (DeviceNotification notification : list) {
                        resultList.add(new NotificationPollManyResponse(notification, notification.getDevice().getGuid()));
                    }
                    asyncResponse.resume(ResponseFactory.response(Response.Status.OK, resultList, Policy.NOTIFICATION_TO_CLIENT));
                } catch (Exception e) {
                    logger.error("Error: " + e.getMessage(), e);
                    asyncResponse.resume(e);
                }
            }
        });
    }


    private List<DeviceNotification> getOrWaitForNotifications(HivePrincipal principal,
                                                                 SubscriptionFilterInternal subscriptionFilter,
                                                                 long timeout) {
        logger.debug("Device notification pollMany requested for : {}.  Timeout = {}",subscriptionFilter, timeout);

        if (subscriptionFilter.getTimestamp() == null) {
            subscriptionFilter.setTimestamp(timestampService.getTimestamp());
        }
        List<DeviceNotification> list = deviceNotificationService.getDeviceNotificationList(subscriptionFilter, principal);

        if (list.isEmpty()) {
            NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
            String reqId = UUID.randomUUID().toString();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            Set<NotificationSubscription> subscriptionSet = new HashSet<>();
            if (subscriptionFilter.getDeviceNames() != null ) {
                Map<Device, Set<String>> filters = deviceService.createFilterMap(subscriptionFilter.getDeviceNames(), principal);
                for (Map.Entry<Device, Set<String>> entry : filters.entrySet()) {
                    subscriptionSet
                            .add(new NotificationSubscription(principal, entry.getKey().getId(), reqId, entry.getValue(),
                                    restHandlerCreator));
                }
            } else {
                subscriptionSet
                        .add(new NotificationSubscription(principal, Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE,
                                reqId,
                                subscriptionFilter.getNames(),
                                restHandlerCreator));
            }

            if (SimpleWaiter
                    .subscribeAndWait(storage, subscriptionSet, restHandlerCreator.getFutureTask(), timeout)) {
                list = deviceNotificationService.getDeviceNotificationList(subscriptionFilter, principal);
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
    public Response insert(@PathParam("deviceGuid") String guid,
                           @JsonPolicyDef(NOTIFICATION_FROM_DEVICE) DeviceNotification notification) {
        logger.debug("DeviceNotification insertAll requested");
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (notification == null || notification.getNotification() == null) {
            logger.debug(
                    "DeviceNotification insertAll proceed with error. Bad notification: notification is required.");
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null) {
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), "No device with such guid : " + guid + " exists"));
        }
        if (device.getNetwork() == null) {
            return ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(FORBIDDEN.getStatusCode(), "No access to device"));
        }
        notificationService.submitDeviceNotification(notification, device);

        logger.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(CREATED, notification, NOTIFICATION_TO_DEVICE);
    }

    @PreDestroy
    public void shutdownThreads() {
        logger.debug("Try to shutdown device notifications' pool");
        asyncPool.shutdown();
        logger.debug("Device notifications' pool has been shut down");
    }

    @PostConstruct
    public void initPool() {
        asyncPool = Executors.newCachedThreadPool();
    }

}