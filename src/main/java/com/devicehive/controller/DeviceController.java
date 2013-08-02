package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;

/**
 * REST controller for devices: <i>/device</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/Device">DeviceHive RESTful API: Device</a> for details.
 */
@Path("/device")
public class DeviceController {

    @Inject
    private DeviceService deviceService;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private DeviceCommandDAO commandDAO;
    @Inject
    private DeviceNotificationDAO notificationDAO;
    @Inject
    private DeviceEquipmentDAO equipmentDAO;
    @Inject
    private DeviceEquipmentDAO deviceEquipmentDAO;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/list"> DeviceHive RESTful API:
     * Device: list</a>
     *
     * @param name               Device name.
     * @param namePattern        Device name pattern.
     * @param status             Device status
     * @param networkId          Associated network identifier
     * @param networkName        Associated network name
     * @param deviceClassId      Associated device class identifier
     * @param deviceClassName    Associated device class name
     * @param deviceClassVersion Associated device class version
     * @param sortField          Result list sort field. Available values are Name, Status, Network and DeviceClass.
     * @param sortOrder          Result list sort order. Available values are ASC and DESC.
     * @param take               Number of records to take from the result list.
     * @param skip               Number of records to skip from the result list.
     * @return list of <a href="http://www.devicehive.com/restful#Reference/Device">Devices</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response list(@QueryParam("name") String name,
                         @QueryParam("namePattern") String namePattern,
                         @QueryParam("status") String status,
                         @QueryParam("networkId") Long networkId,
                         @QueryParam("networkName") String networkName,
                         @QueryParam("deviceClassId") Long deviceClassId,
                         @QueryParam("deviceClassName") String deviceClassName,
                         @QueryParam("deviceClassVersion") String deviceClassVersion,
                         @QueryParam("sortField") String sortField,
                         @QueryParam("sortOrder") String sortOrder,
                         @QueryParam("take") Integer take,
                         @QueryParam("skip") Integer skip) {

        boolean sortOrderAsc = true;

        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse("Invalid request parameters"));
        }
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Name".equals(sortField) && !"Status".equals(sortField) && !"Network".equals(sortField) &&
                !"DeviceClass".equals(sortField) && sortField != null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse("Invalid request parameters"));
        }

        List<Device> result = deviceDAO.getList(name, namePattern, status, networkId, networkName, deviceClassId,
                deviceClassName, deviceClassVersion, sortField, sortOrderAsc, take, skip);

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.DEVICE_PUBLISHED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/register">DeviceHive RESTful
     * API: Device: register</a>
     * Registers a device.
     * If device with specified identifier has already been registered,
     * it gets updated in case when valid key is provided in the authorization header.
     *
     * @param jsonObject In the request body, supply a Device resource. See <a href="http://www.devicehive
     *                   .com/restful#Reference/Device/register">
     * @param guid       Device unique identifier.
     * @return response code 201, if successful
     */
    @PUT
    @Path("/{id}")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICE_SUBMITTED)
    public Response register(JsonObject jsonObject, @PathParam("id") String guid) {

        UUID deviceGuid = null;

        try {
            deviceGuid = UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse("Invalid request parameters."));
        }

        Gson mainGson = GsonFactory.createGson(DEVICE_PUBLISHED);
        DeviceUpdate device = null;

        device = mainGson.fromJson(jsonObject, DeviceUpdate.class);

        NullableWrapper<UUID> uuidNullableWrapper = new NullableWrapper<>();

        uuidNullableWrapper.setValue(deviceGuid);
        device.setGuid(uuidNullableWrapper);

        try {
            deviceService.checkDevice(device);
        } catch (HiveException e) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse("Invalid request parameters."));
        }

        Gson gsonForEquipment = GsonFactory.createGson();
        boolean useExistingEquipment = jsonObject.get("equipment") == null;
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(
                jsonObject.get("equipment"),
                new TypeToken<HashSet<Equipment>>() {
                }.getType());

        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }

        deviceService.deviceSave(device, equipmentSet, useExistingEquipment);

        return ResponseFactory.response(Response.Status.CREATED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/get">DeviceHive RESTful
     * API: Device: get</a>
     * Gets information about device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns
     *         a <a href="http://www.devicehive.com/restful#Reference/Device">Device</a> resource in the response body.
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    public Response get(@PathParam("id") String guid) {

        Device device = null;

        try {
            device = getDevice(guid);
        } catch (BadRequestException e) {
            return ResponseFactory
                    .response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        } catch (NotFoundException e) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Device not found."));
        }

        return ResponseFactory.response(Response.Status.OK, device, JsonPolicyDef.Policy.DEVICE_PUBLISHED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/delete">DeviceHive RESTful
     * API: Device: delete</a>
     * Deletes an existing device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns an empty response body.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response delete(@PathParam("id") String guid) {

        UUID deviceId;

        try {
            deviceId = UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse("Invalid request parameters"));
        }

        deviceDAO.deleteDevice(deviceId);

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/equipment">DeviceHive RESTful
     * API: Device: equipment</a>
     * Gets current state of device equipment.
     * The equipment state is tracked by framework and it could be updated by sending 'equipment' notification
     * with the following parameters:
     * equipment: equipment code
     * parameters: current equipment state
     *
     * @param guid Device unique identifier.
     * @return If successful, this method returns array of the following structures in the response body.
     *         <table>
     *         <tr>
     *         <td>Property Name</td>
     *         <td>Type</td>
     *         <td>Description</td>
     *         </tr>
     *         <tr>
     *         <td>id</td>
     *         <td>string</td>
     *         <td>Equipment code.</td>
     *         </tr>
     *         <tr>
     *         <td>timestamp</td>
     *         <td>datetime</td>
     *         <td>Equipment state timestamp.</td>
     *         </tr>
     *         <tr>
     *         <td>parameters</td>
     *         <td>object</td>
     *         <td>Current equipment state.</td>
     *         </tr>
     *         </table>
     */
    @GET
    @Path("/{id}/equipment")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response equipment(@PathParam("id") String guid) {

        Device device = null;

        try {
            device = getDevice(guid);
        } catch (BadRequestException e) {
            return ResponseFactory
                    .response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        } catch (NotFoundException e) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Device not found."));
        }

        List<DeviceEquipment> equipments = equipmentDAO.findByFK(device);

        return ResponseFactory
                .response(Response.Status.OK, equipments, JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED);
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
