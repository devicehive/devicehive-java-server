package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.handler.RestHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscriptionStorage;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.RestParametersConverter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

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
    private static final int STOP_TIMEOUT = 5;

    @EJB
    private DeviceNotificationService notificationService;

    @EJB
    private SubscriptionManager subscriptionManager;

    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;

    @EJB
    private DeviceService deviceService;
    private ExecutorService asyncPool;

    @PostConstruct
    public void initPool() {
        asyncPool = Executors.newCachedThreadPool();
    }

    @GET
    @Path("/{deviceGuid}/notification")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response query(@PathParam("deviceGuid") UUID guid,
                          @QueryParam("start") String start,
                          @QueryParam("end") String end,
                          @QueryParam("notification") String notification,
                          @QueryParam("sortField") String sortField,
                          @QueryParam("sortOrder") String sortOrder,
                          @QueryParam("take") Integer take,
                          @QueryParam("skip") Integer skip,
                          @Context SecurityContext securityContext) {

        logger.debug("Device notification requested");

        Boolean sortOrderAsc = RestParametersConverter.isSortAsc(sortOrder);

        if (sortOrderAsc == null) {
            logger.debug("Device notification request failed. Bad request for sortOrder.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.WRONG_SORT_ORDER_PARAM_MESSAGE));
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

        Timestamp startTimestamp = null, endTimestamp = null;

        if (start != null) {
            startTimestamp = TimestampAdapter.parseTimestampQuietly(start);
            if (startTimestamp == null) {
                logger.debug("Device notification request failed. Unparseable timestamp.");
                return ResponseFactory.response(Response.Status.BAD_REQUEST,
                        new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
            }
        }
        if (end != null) {
            endTimestamp = TimestampAdapter.parseTimestampQuietly(end);
            if (endTimestamp == null) {
                logger.debug("Device notification request failed. Unparseable timestamp.");
                return ResponseFactory.response(Response.Status.BAD_REQUEST,
                        new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
            }
        }
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Device device = deviceService.getDevice(guid, principal.getUser(), principal.getDevice());
        List<DeviceNotification> result = notificationService.queryDeviceNotification(device, startTimestamp,
                endTimestamp, notification, sortField, sortOrderAsc, take, skip);

        logger.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, result, Policy.NOTIFICATION_TO_CLIENT);
    }

    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response get(@PathParam("deviceGuid") UUID guid, @PathParam("id") Long notificationId,
                        @Context SecurityContext securityContext) {
        logger.debug("Device notification requested");

        DeviceNotification deviceNotification = notificationService.findById(notificationId);
        if (deviceNotification == null) {
            throw new HiveException("Device notification with id : " + notificationId + " not found",
                    NOT_FOUND.getStatusCode());
        }
        UUID deviceGuidFromNotification = deviceNotification.getDevice().getGuid();
        if (!deviceGuidFromNotification.equals(guid)) {
            logger.debug("No device notifications found for device with guid : " + guid);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse("No device notifications " +
                    "found for device with guid : " + guid));
        }
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        if (!deviceService
                .checkPermissions(deviceNotification.getDevice(), principal.getUser(), principal.getDevice())) {
            logger.debug("No permissions to get notifications for device with guid : " + guid);
            return ResponseFactory.response(Response.Status.UNAUTHORIZED, new ErrorResponse("Unauthorized"));
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
            @PathParam("deviceGuid") UUID deviceGuid,
            @QueryParam("timestamp") Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT) @QueryParam
                    ("waitTimeout") long timeout,
            @Context SecurityContext securityContext,
            @Suspended AsyncResponse asyncResponse) {

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        logger.debug("Device notification poll requested for device with guid = " + deviceGuid + ". " +
                "Timestamp = " + timestamp + ". Timeout = " + timeout);

        if (deviceGuid == null) {
            logger.debug("Device notification poll finished with error. No device guid specified");
            asyncResponse.resume(
                    ResponseFactory.response(NOT_FOUND, new ErrorResponse("No device with guid = " +
                            deviceGuid + " found")));
        }

        Device device = deviceService.getDevice(deviceGuid, principal.getUser(), principal.getDevice());
        User user = principal.getUser();

        List<DeviceNotification> list = deviceNotificationDAO.getByUserNewerThan(user, timestamp);
        if (list.isEmpty()) {
            logger.debug("Waiting for command for device = " + deviceGuid);
            if (pollAction(timeout, user, device)) {
                list = deviceNotificationDAO.getByUserNewerThan(user, timestamp);
            }
        }
        logger.debug("Device notification poll proceed successfully for device with guid = " + deviceGuid);
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, list,
                Policy.NOTIFICATION_TO_CLIENT));
        logger.debug("Device notification poll proceed successfully for device with guid = " + deviceGuid);

        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, list,
                Policy.NOTIFICATION_TO_CLIENT));
    }

    private boolean pollAction(final long timeout, final User user, final Device device) {
        Future<Boolean> result = asyncPool.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
                String reqId = UUID.randomUUID().toString();
                RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
                NotificationSubscription notificationSubscription =
                        new NotificationSubscription(user, device.getId(), reqId, restHandlerCreator);

                if (SimpleWaiter
                        .subscribeAndWait(storage, notificationSubscription, restHandlerCreator.getFutureTask(),
                                timeout)) {
                    return true;
                }
                return false;
            }
        });
        try {
            return result.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new HiveException("Unable to poll");
        }
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
            @QueryParam("deviceGuids") String deviceGuids,
            @QueryParam("timestamp") Timestamp timestamp,
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT) @Min(0) @Max(Constants.MAX_WAIT_TIMEOUT)
            @QueryParam("waitTimeout") long timeout,
            @Context SecurityContext securityContext,
            @Suspended AsyncResponse asyncResponse) {


        logger.debug("Device notification pollMany requested for devices : " + deviceGuids + ". Timestamp : "
                + timestamp + ". Timeout = " + timeout);

        List<String> guids =
                deviceGuids == null ? Collections.<String>emptyList() : Arrays.asList(deviceGuids.split(","));
        List<UUID> uuids = new ArrayList<>(guids.size());
        try {
            for (String guid : guids) {
                if (StringUtils.isNotBlank(guid)) {
                    uuids.add(UUID.fromString(guid));
                }
            }
        } catch (IllegalArgumentException e) {
            logger.debug("Device notification pollMany failed. Unparseable guid : " + deviceGuids);
            asyncResponse.resume(ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE)));
        }

        List<Device> devices;

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        if (user.getRole().equals(UserRole.ADMIN)) {
            devices = deviceService.findByUUID(uuids);
        } else {
            devices = deviceService.findByUUIDListAndUser(user, uuids);
        }

        List<DeviceNotification> list = deviceNotificationDAO.getByUserNewerThan(user, timestamp);
        if (list.isEmpty()) {
            logger.debug("Waiting for command");


            if (pollManyAction(devices, timeout, user)) {
                list = deviceNotificationDAO.getByUserNewerThan(user, timestamp);
            }

        }
        logger.debug("Device notification pollMany proceed successfully for devices : " + deviceGuids);
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, list, Policy.NOTIFICATION_TO_CLIENT));
    }

    private boolean pollManyAction(final List<Device> devices, final long timeout, final User user) {
        Future<Boolean> result = asyncPool.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {

                NotificationSubscriptionStorage storage = subscriptionManager.getNotificationSubscriptionStorage();
                String reqId = UUID.randomUUID().toString();
                RestHandlerCreator restHandlerCreator = new RestHandlerCreator();
                Set<NotificationSubscription> subscriptionSet = new HashSet<>();

                for (Device device : devices) {
                    subscriptionSet
                            .add(new NotificationSubscription(user, device.getId(), reqId, restHandlerCreator));
                }


                if (SimpleWaiter
                        .subscribeAndWait(storage, subscriptionSet, restHandlerCreator.getFutureTask(), timeout)) {
                    return true;
                }
                return false;
            }

        });
        try {
            return result.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new HiveException("Unable to poll");
        }
    }

    @POST
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") UUID guid, JsonObject jsonObject,
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
        Device device = deviceService.getDevice(guid, principal.getUser(), principal.getDevice());
        if (device.getNetwork() == null) {
            logger.debug("DeviceNotification insertAll proceed with error. No network specified for device with guid = "
                    + guid);
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("No access to device"));
        }
        deviceService.submitDeviceNotification(notification, device, null);

        logger.debug("DeviceNotification insertAll proceed successfully");
        return ResponseFactory.response(Response.Status.CREATED, notification, Policy.NOTIFICATION_TO_DEVICE);
    }

    @PreDestroy
    public void shutdownThreads() {
        logger.debug("Try to shutdown device notifications' pool");
        asyncPool.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!asyncPool.awaitTermination(STOP_TIMEOUT, TimeUnit.SECONDS)) {
                asyncPool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!asyncPool.awaitTermination(STOP_TIMEOUT, TimeUnit.SECONDS))
                    logger.error("device notifications pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            asyncPool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            logger.warn("device notification pool has been terminated");
        }
        logger.debug("Device notifications' pool has been shut down");
    }

}