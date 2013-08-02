package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.DeferredResponse;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.util.Params;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.service.DeviceService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * REST controller for device notifications: <i>/device/{deviceGuid}/notification</i> and <i>/device/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API: DeviceNotification</a> for details.
 *
 * @author rroschin
 */
@Path("/device")
public class DeviceNotificationController {

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@PathParam("deviceGuid") String guid,
                          @QueryParam("start") String start,
                          @QueryParam("end") String end,
                          @QueryParam("notification") String notification,
                          @QueryParam("sortField") String sortField,
                          @QueryParam("sortOrder") String sortOrder,
                          @QueryParam("take") Integer take,
                          @QueryParam("skip") Integer skip) {
        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        boolean sortOrderAsc = true;
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Timestamp".equals(sortField) && !"Notification".equals(sortField) && sortField != null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (sortField == null) {
            sortField = "timestamp";
        }
        sortField = sortField.toLowerCase();

        Date startTimestamp = null, endTimestamp = null;

        if (start != null) {
            startTimestamp = Params.parseUTCDate(start);
            if (startTimestamp == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        if (end != null) {
            endTimestamp = Params.parseUTCDate(end);
            if (endTimestamp == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        Device device = getDevice(guid);
        Annotation[] annotations =
                {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT)};
        List<DeviceNotification> result = notificationDAO.queryDeviceNotification(device, startTimestamp,
                endTimestamp, notification, sortField, sortOrderAsc, take, skip);
        return Response.ok().entity(result, annotations).build();
    }

    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("deviceGuid") String guid, @PathParam("id") Long notificationId) {
        DeviceNotification deviceNotification = notificationDAO.findById(notificationId);
        String deviceGuidFromNotification = deviceNotification.getDevice().getGuid().toString();
        if (!deviceGuidFromNotification.equals(guid)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Annotation[] annotations =
                {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT)};
        return Response.ok().entity(deviceNotification, annotations).build();
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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{deviceGuid}/notification/poll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        if (deviceGuid == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Device device = deviceDAO.findByUUID(UUID.fromString(deviceGuid));
        if (device == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Date timestamp = Params.parseUTCDate(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();

        DeferredResponse result = messageBus.subscribe(MessageType.DEVICE_TO_CLIENT_NOTIFICATION,
                MessageDetails.create().ids(device.getId()).timestamp(timestamp).user(user));
        List<DeviceNotification> response =
                MessageBus.expandDeferredResponse(result, timeout, DeviceNotification.class);
        Annotation[] annotations =
                {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT)};
        return Response.ok().entity(response, annotations).build();
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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/notification/poll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pollMany(
            @QueryParam("deviceGuids") String deviceGuids,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout,
            @Context SecurityContext securityContext) {

        List<String> guids =
                deviceGuids == null ? Collections.<String>emptyList() : Arrays.asList(deviceGuids.split(","));
        List<UUID> uuids = new ArrayList<>(guids.size());
        for (String guid : guids) {
            uuids.add(UUID.fromString(guid));
        }

        List<Device> devices = deviceDAO.findByUUID(uuids);
        List<Long> ids = new ArrayList<>(devices.size());
        for (Device device : devices) {
            ids.add(device.getId());
        }

        Date timestamp = Params.parseUTCDate(timestampUTC);
        long timeout = Params.parseWaitTimeout(waitTimeout);

        User user = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();

        DeferredResponse result = messageBus.subscribe(MessageType.DEVICE_TO_CLIENT_NOTIFICATION,
                MessageDetails.create().ids(ids).timestamp(timestamp).user(user));
        List<DeviceNotification> response =
                MessageBus.expandDeferredResponse(result, timeout, DeviceNotification.class);
        Annotation[] annotations =
                {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT)};
        return Response.ok().entity(response, annotations).build();
    }

    @POST
    @RolesAllowed({HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Path("/{deviceGuid}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert (@PathParam("deviceGuid") String guid, JsonObject jsonObject){
        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE);
        DeviceNotification notification = gson.fromJson(jsonObject, DeviceNotification.class);
        Device device = getDevice(guid);
        if (device.getNetwork() == null){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (notification == null || notification.getNotification() == null){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        deviceService.submitDeviceNotification(notification, device, null);
        Annotation[] annotations =
                {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE)};
        return Response.status(Response.Status.CREATED).type(MediaType.APPLICATION_JSON).entity(notification, annotations).build();

    }


    private Device getDevice(String uuid) {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unparseable guid: " + uuid);
        }
        Device device = deviceDAO.findByUUID(deviceId);
        if (device == null) {
            throw new NotFoundException("device with guid " + uuid + " not found");
        }
        return device;
    }
}
