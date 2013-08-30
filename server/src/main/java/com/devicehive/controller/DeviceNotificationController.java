package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.model.UserRole;
import com.devicehive.model.response.NotificationPollManyResponse;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

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
    @EJB
    private DeviceNotificationService notificationService;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private GlobalMessageBus globalMessageBus;
    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;
    @EJB
    private DeviceService deviceService;
    @EJB
    private TimestampService timestampService;
    private ExecutorService asyncPool;

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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response query(@PathParam("deviceGuid") String guid,
                          @QueryParam("start") Timestamp start,
                          @QueryParam("end") Timestamp end,
                          @QueryParam("notification") String notification,
                          @QueryParam("sortField") String sortField,
                          @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                          @QueryParam("take") Integer take,
                          @QueryParam("skip") Integer skip,
                          @Context SecurityContext securityContext) {

        logger.debug("Device notification requested");

        if (sortOrder == null) {
            sortOrder = true;
        }

        if (!"Timestamp".equals(sortField) && !"Notification".equals(sortField) && sortField != null) {
            logger.debug("Device notification request failed. Bad request for sortField.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }

        if (sortField == null) {
            sortField = "timestamp";
        }

        sortField = sortField.toLowerCase();

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Device device = deviceService.getDevice(guid, principal.getUser(),
                principal.getDevice());
        List<DeviceNotification> result = notificationService.queryDeviceNotification(device, start, end,
                notification, sortField, sortOrder, take, skip);

        logger.debug("Device notification proceed successfully");

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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") Long notificationId,
                        @Context SecurityContext securityContext) {
        logger.debug("Device notification requested");

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
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        if (!deviceService
                .checkPermissions(deviceNotification.getDevice(), principal.getUser(), principal.getDevice())) {
            logger.debug("No permissions to get notifications for device with guid : {}", guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("No device notifications " +
                    "found for device with guid : " + guid));
        }

        logger.debug("Device notification proceed successfully");


        return ResponseFactory.response(Response.Status.OK, deviceNotification, Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/poll">DeviceHive RESTful API: DeviceNotification: poll</a>
     *
     * @param deviceGuid Device unique identifier.
     * @param timestamp  Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param timeout    Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE})
    @Path("/{deviceGuid}/notification/poll")
    public void poll(
            @PathParam("deviceGuid") final String deviceGuid,
            @QueryParam("timestamp") final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT) @QueryParam
                    ("waitTimeout") final long timeout,
            @Context final ContainerRequestContext requestContext,
            @Suspended final AsyncResponse asyncResponse) {

        final HivePrincipal principal = (HivePrincipal) requestContext.getSecurityContext().getUserPrincipal();

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("Device notification poll proceed successfully for device with guid = {}", deviceGuid);
            }
        });
        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                asyncResponsePollProcess(timestamp, deviceGuid, timeout, principal, asyncResponse);
            }
        });
    }

    private void asyncResponsePollProcess(Timestamp timestamp, String deviceGuid, long timeout,
                                          HivePrincipal principal, AsyncResponse asyncResponse) {
        logger.debug("Device notification poll requested for device with guid = {}. Timestamp = {}. Timeout = {}",
                deviceGuid, timestamp, timeout);

        if (deviceGuid == null) {
            logger.debug("Device notification poll finished with error. No device guid specified");

            asyncResponse.resume(
                    ResponseFactory.response(NOT_FOUND, new ErrorResponse("No device guid specified")));
            return;
        }
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        User user = principal.getUser();
        List<DeviceNotification> list = getDeviceNotificationsList(user, deviceGuid, timestamp);
        if (list.isEmpty()) {
            logger.debug("Waiting for notification from device = {}", deviceGuid);
            NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
            String reqId = UUID.randomUUID().toString();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            Device device = deviceService.getDevice(deviceGuid, principal.getUser(),
                    principal.getDevice());
            NotificationSubscription notificationSubscription =
                    new NotificationSubscription(user, device.getId(), reqId, restHandlerCreator);

            if (SimpleWaiter
                    .subscribeAndWait(storage, notificationSubscription, restHandlerCreator.getFutureTask(),
                            timeout)) {
                list = getDeviceNotificationsList(user, deviceGuid, timestamp);
            }
        }
        Response response = ResponseFactory.response(Response.Status.OK, list,
                Policy.NOTIFICATION_TO_CLIENT);

        asyncResponse.resume(response);
    }

    private List<DeviceNotification> getDeviceNotificationsList(User user, String guid, Timestamp timestamp) {
        List<String> guidList = new ArrayList<>(1);
        guidList.add(guid);
        if (user.getRole().equals(UserRole.CLIENT)) {
            return deviceNotificationDAO.getByUserAndDevicesNewerThan(user, guidList, timestamp);
        }
        return deviceNotificationDAO.findByDevicesIdsNewerThan(guidList, timestamp);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/pollMany">DeviceHive RESTful API: DeviceNotification: pollMany</a>
     *
     * @param deviceGuids Device unique identifier.
     * @param timestamp   Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param timeout     Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    @Path("/notification/poll")
    public void pollMany(
            @QueryParam("deviceGuids") final String deviceGuids,
            @QueryParam("timestamp") final Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") final long timeout,
            @Context SecurityContext securityContext,
            @Suspended final AsyncResponse asyncResponse) {

        final User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("Device notification poll many proceed successfully for devices: {}", deviceGuids);
            }
        });

        asyncPool.submit(new Runnable() {
            @Override
            public void run() {
                asyncResponsePollManyProcess(deviceGuids, timestamp, timeout, user, asyncResponse);
            }
        });
    }

    private void asyncResponsePollManyProcess(String deviceGuids, Timestamp timestamp, long timeout,
                                              User user, AsyncResponse asyncResponse) {
        logger.debug("Device notification pollMany requested for devices: {}. Timestamp: {}. Timeout = {}",
                deviceGuids, timestamp, timeout);

        List<String> guids =
                deviceGuids == null ? Collections.<String>emptyList() : Arrays.asList(deviceGuids.split(","));

        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        List<DeviceNotification> list = getDeviceNotificationsList(user, guids, timestamp);

        if (list.isEmpty()) {
            NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
            String reqId = UUID.randomUUID().toString();
            RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
            Set<NotificationSubscription> subscriptionSet = new HashSet<>();
            if (!guids.isEmpty()) {
                List<Device> devices;

                if (user.getRole().equals(UserRole.ADMIN)) {
                    devices = deviceService.findByUUID(guids);
                } else {
                    devices = deviceService.findByUUIDListAndUser(user, guids);
                }
                for (Device device : devices) {
                    subscriptionSet
                            .add(new NotificationSubscription(user, device.getId(), reqId, restHandlerCreator));
                }
            } else {
                subscriptionSet
                        .add(new NotificationSubscription(user, Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE, reqId,
                                restHandlerCreator));
            }

            if (SimpleWaiter
                    .subscribeAndWait(storage, subscriptionSet, restHandlerCreator.getFutureTask(), timeout)) {
                list = getDeviceNotificationsList(user, guids, timestamp);
            }
            List<NotificationPollManyResponse> resultList = new ArrayList<>(list.size());
            for (DeviceNotification notification : list) {
                resultList.add(new NotificationPollManyResponse(notification, notification.getDevice().getGuid()));
            }

            asyncResponse
                    .resume(ResponseFactory.response(Response.Status.OK, resultList, Policy.NOTIFICATION_TO_CLIENT));
            return;
        }
        List<NotificationPollManyResponse> resultList = new ArrayList<>(list.size());
        for (DeviceNotification notification : list) {
            resultList.add(new NotificationPollManyResponse(notification, notification.getDevice().getGuid()));
        }
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, resultList, Policy.NOTIFICATION_TO_CLIENT));
    }

    private List<DeviceNotification> getDeviceNotificationsList(User user, List<String> guids, Timestamp timestamp) {
        if (!guids.isEmpty()) {
            return user.getRole().equals(UserRole.CLIENT) ?
                    deviceNotificationDAO.getByUserAndDevicesNewerThan(user, guids, timestamp) :
                    deviceNotificationDAO.findByDevicesIdsNewerThan(guids, timestamp);
        } else {
            return user.getRole().equals(UserRole.CLIENT) ?
                    deviceNotificationDAO.getByUserNewerThan(user, timestamp) :
                    deviceNotificationDAO.findNewerThan(timestamp);
        }
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/insert">DeviceHive
     * RESTful API: DeviceNotification: insert</a>
     * Creates new device notification.
     *
     * @param guid       Device unique identifier.
     * @param jsonObject In the request body, supply a DeviceNotification resource.
     *                   <table>
     *                   <tr>
     *                   <td>Property Name</td>
     *                   <td>Required</td>
     *                   <td>Type</td>
     *                   <td>Description</td>
     *                   </tr>
     *                   <tr>
     *                   <td>notification</td>
     *                   <td>Yes</td>
     *                   <td>string</td>
     *                   <td>Notification name.</td>
     *                   </tr>
     *                   <tr>
     *                   <td>parameters</td>
     *                   <td>No</td>
     *                   <td>object</td>
     *                   <td>Notification parameters, a JSON object with an arbitrary structure.</td>
     *                   </tr>
     *                   </table>
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
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") String guid, JsonObject jsonObject,
                           @Context SecurityContext securityContext) {
        logger.debug("DeviceNotification insertAll requested");

        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE);
        DeviceNotification notification = gson.fromJson(jsonObject, DeviceNotification.class);
        if (notification == null || notification.getNotification() == null) {
            logger.debug(
                    "DeviceNotification insertAll proceed with error. Bad notification: notification is required.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Device device = deviceService.getDevice(guid, principal.getUser(),
                principal.getDevice());
        if (device.getNetwork() == null) {
            logger.debug(
                    "DeviceNotification insertAll proceed with error. No network specified for device with guid = {}",
                    guid);
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("No access to device"));
        }
        notificationService.submitDeviceNotification(notification, device);

        logger.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(Response.Status.CREATED, notification, Policy.NOTIFICATION_TO_DEVICE);
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