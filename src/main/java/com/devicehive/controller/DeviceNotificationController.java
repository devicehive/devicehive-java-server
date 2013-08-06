package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.DeferredResponse;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.util.Params;
import com.devicehive.model.*;
import com.devicehive.service.DeviceService;
import com.devicehive.utils.RestParametersConverter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API: DeviceNotification</a> for details.
 *
 * @author rroschin
 */
@Path("/device")
public class DeviceNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationController.class);

    @Inject
    private DeviceNotificationDAO notificationDAO;

    @Inject
    private DeviceDAO deviceDAO;

    @Inject
    private MessageBus messageBus;

    @Inject
    private DeviceService deviceService;

    @GET
    @Path("/{deviceGuid}/notification")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response query(@PathParam("deviceGuid") String guid,
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
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.WRONG_SORT_ORDER_PARAM_MESSAGE));
        }


        if (!"Timestamp".equals(sortField) && !"Notification".equals(sortField) && sortField != null) {
            logger.debug("Device notification request failed. Bad request for sortField.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
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
                return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
            }
        }
        if (end != null) {
            endTimestamp = TimestampAdapter.parseTimestampQuietly(end);
            if (endTimestamp == null) {
                logger.debug("Device notification request failed. Unparseable timestamp.");
                return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
            }
        }

        Device device = deviceService.getDevice(guid, (HivePrincipal) securityContext.getUserPrincipal());
        List<DeviceNotification> result = notificationDAO.queryDeviceNotification(device, startTimestamp,
                endTimestamp, notification, sortField, sortOrderAsc, take, skip);

        logger.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, result, Policy.NOTIFICATION_TO_CLIENT);
    }

    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") Long notificationId,
                        @Context SecurityContext securityContext) {

        logger.debug("Device notification requested");

        DeviceNotification deviceNotification = notificationDAO.findById(notificationId);
        String deviceGuidFromNotification = deviceNotification.getDevice().getGuid().toString();
        if (!deviceGuidFromNotification.equals(guid)) {
            logger.debug("No device notifications found for device with guid = " + guid);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("No device notifications found for device with guid = " + guid));
        }
        if (!deviceService.checkPermissions(deviceNotification.getDevice(), (HivePrincipal) securityContext
                .getUserPrincipal())) {
            logger.debug("No permissions to get notifications for device with guid = " + guid);
            return ResponseFactory.response(Response.Status.UNAUTHORIZED, new ErrorResponse("Unauthorized"));
        }
        logger.debug("Device notification proceed successfully");

        return ResponseFactory.response(Response.Status.OK, deviceNotification, Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/poll">DeviceHive RESTful API: DeviceNotification: poll</a>
     *
     * @param deviceGuid   Device unique identifier.
     * @param timestampUTC Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param waitTimeout  Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    @Path("/{deviceGuid}/notification/poll")
    public Response poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        logger.debug("Device notification poll requested");

        if (deviceGuid == null) {
            logger.debug("Device notification poll finished with error. No device guid specified");
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("No device with guid = " +
                    deviceGuid + " found"));
        }

        Device device = deviceService.getDevice(deviceGuid, (HivePrincipal) securityContext.getUserPrincipal());

        Timestamp timestamp = TimestampAdapter.parseTimestampQuietly(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();

        DeferredResponse result = messageBus.subscribe(MessageType.DEVICE_TO_CLIENT_NOTIFICATION,
                MessageDetails.create().ids(device.getId()).timestamp(timestamp).user(user));
        List<DeviceNotification> response =
                MessageBus.expandDeferredResponse(result, timeout, DeviceNotification.class);
        return ResponseFactory.response(Response.Status.OK, response, Policy.NOTIFICATION_TO_CLIENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/pollMany">DeviceHive RESTful API: DeviceNotification: pollMany</a>
     *
     * @param deviceGuids  Device unique identifier.
     * @param timestampUTC Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     * @param waitTimeout  Waiting timeout in seconds (default: 30 seconds, maximum: 60 seconds). Specify 0 to disable waiting.
     * @return Array of <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceNotification</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    @Path("/notification/poll")
    public Response pollMany(
            @QueryParam("deviceGuids") String deviceGuids,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        logger.debug("Device notification pollMany requested");

        List<String> guids =
                deviceGuids == null ? Collections.<String>emptyList() : Arrays.asList(deviceGuids.split(","));
        List<UUID> uuids = new ArrayList<>(guids.size());
        try {
            for (String guid : guids) {
                uuids.add(UUID.fromString(guid));
            }
        } catch (IllegalArgumentException e) {
            logger.debug("Device notification pollMany failed. Unparseable guid.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        List<Device> devices;
        if (user.getRole().equals(UserRole.ADMIN)) {
            devices = deviceDAO.findByUUID(uuids);
        } else {
            devices = deviceDAO.findByUUIDListAndUser(user, uuids);
        }

        List<Long> ids = new ArrayList<>(devices.size());
        for (Device device : devices) {
            ids.add(device.getId());
        }

        Timestamp timestamp = TimestampAdapter.parseTimestampQuietly(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);


        DeferredResponse result = messageBus.subscribe(MessageType.DEVICE_TO_CLIENT_NOTIFICATION,
                MessageDetails.create().ids(ids).timestamp(timestamp).user(user));
        List<DeviceNotification> response =
                MessageBus.expandDeferredResponse(result, timeout, DeviceNotification.class);
        logger.debug("Device notification pollMany proceed successfully");
        return ResponseFactory.response(Response.Status.OK, response, Policy.NOTIFICATION_TO_CLIENT);
    }

    @POST
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(@PathParam("deviceGuid") String guid, JsonObject jsonObject,
                           @Context SecurityContext securityContext) {
        logger.debug("DeviceNotification insert requested");

        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE);
        DeviceNotification notification = gson.fromJson(jsonObject, DeviceNotification.class);
        if (notification == null || notification.getNotification() == null) {
            logger.debug("DeviceNotification insert proceed with error. Bad notification: notification is required.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        Device device = deviceService.getDevice(guid, (HivePrincipal) securityContext.getUserPrincipal());
        if (device.getNetwork() == null) {
            logger.debug("DeviceNotification insert proceed with error. No network specified for device with guid = "
                    + guid);
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("No access to device"));
        }
        deviceService.submitDeviceNotification(notification, device, null);

        logger.debug("DeviceNotification insert proceed successfully");
        return ResponseFactory.response(Response.Status.CREATED, notification, Policy.NOTIFICATION_TO_DEVICE);
    }

}