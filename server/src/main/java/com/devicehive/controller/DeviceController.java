package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.Equipment;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceEquipmentService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
@LogExecutionTime
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    @EJB
    private DeviceEquipmentService deviceEquipmentService;
    @EJB
    private DeviceCommandService deviceCommandService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private UserService userService;

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
                         @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                         @QueryParam("take") Integer take,
                         @QueryParam("skip") Integer skip,
                         @Context SecurityContext securityContext) {

        logger.debug("Device list requested");

        if (sortOrder == null) {
            sortOrder = true;
        }
        if (!"Name".equals(sortField) && !"Status".equals(sortField) && !"Network".equals(sortField) &&
                !"DeviceClass".equals(sortField) && sortField != null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        User currentUser = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();

        List<Device> result = deviceService.getList(name, namePattern, status, networkId, networkName, deviceClassId,
                deviceClassName, deviceClassVersion, sortField, sortOrder, take, skip, currentUser);

        logger.debug("Device list proceed result. Result list contains {} elems", result.size());

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
     * @param deviceGuid Device unique identifier.
     * @return response code 201, if successful
     */
    @PUT
    @Path("/{id}")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(JsonObject jsonObject, @PathParam("id") String deviceGuid,
                             @Context SecurityContext securityContext) {
        logger.debug("Device register method requested");

        Gson mainGson = GsonFactory.createGson(DEVICE_PUBLISHED);
        DeviceUpdate device;

        device = mainGson.fromJson(jsonObject, DeviceUpdate.class);

        device.setGuid(new NullableWrapper<>(deviceGuid));

        try {
            deviceService.checkDevice(device);
        } catch (HiveException e) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
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

        User currentUser = ((HivePrincipal) securityContext.getUserPrincipal()).getUser();
        Device currentDevice = ((HivePrincipal) securityContext.getUserPrincipal()).getDevice();
        boolean isAllowedToUpdate = ((currentUser != null && currentUser.isAdmin()) || (currentDevice != null &&
                currentDevice.getGuid().equals(deviceGuid)));
        deviceService.deviceSaveAndNotify(device, equipmentSet, useExistingEquipment, isAllowedToUpdate);
        logger.debug("Device register finished successfully");

        return ResponseFactory.response(Response.Status.NO_CONTENT);
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
    public Response get(@PathParam("id") String guid, @Context SecurityContext securityContext) {
        logger.debug("Device get requested");

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Device device;

        device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal.getUser(), principal.getDevice());

        logger.debug("Device get proceed successfully");

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

        logger.debug("Device delete requested");

        deviceService.deleteDevice(guid);

        logger.debug("Device with id = {} is deleted", guid);

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
    public Response equipment(@PathParam("id") String guid, @Context SecurityContext securityContext) {
        logger.debug("Device equipment requested");

        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal.getUser(),
                principal.getDevice());
        List<DeviceEquipment> equipments = deviceEquipmentService.findByFK(device);

        logger.debug("Device equipment request proceed successfully");

        return ResponseFactory
                .response(Response.Status.OK, equipments, JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED);
    }

    /**
     * Gets current state of device equipment.
     * The equipment state is tracked by framework and it could be updated by sending 'equipment' notification
     * with the following parameters:
     * equipment: equipment code
     * parameters: current equipment state
     *
     * @param guid device guid
     * @param code equipment code
     * @return If successful return equipment associated with code and device with following guid
     */
    @GET
    @Path("/{id}/equipment/{code}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response equipmentByCode(@PathParam("id") String guid,
                                    @PathParam("code") String code,
                                    @Context SecurityContext securityContext) {

        logger.debug("Device equipment by code requested");
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal.getUser(),
                principal.getDevice());
        DeviceEquipment equipment = deviceEquipmentService.findByCodeAndDevice(code, device);
        if (equipment == null) {
            logger.debug("No device equipment found for code : {} and guid : {}", code, guid);
            return ResponseFactory
                    .response(Response.Status.NOT_FOUND, new ErrorResponse(ErrorResponse.DEVICE_NOT_FOUND_MESSAGE));
        }
        logger.debug("Device equipment by code proceed successfully");

        return ResponseFactory
                .response(Response.Status.OK, equipment, JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED);
    }

    /**
     * Will always throw 404
     * Needed for pass unit tests.
     */
    @PUT
    @Path("/{id}/equipment/{code}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response updateByCode(@PathParam("id") UUID guid, @PathParam("code") String code) {
        return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Not Found"));
    }
}
