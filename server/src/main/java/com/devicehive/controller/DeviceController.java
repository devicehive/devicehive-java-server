package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.Equipment;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceEquipmentService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_STATE;
import static com.devicehive.auth.AllowedKeyAction.Action.REGISTER_DEVICE;
import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED_DEVICE_AUTH;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST controller for devices: <i>/device</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/Device">DeviceHive RESTful API: Device</a> for details.
 */
@Path("/device")
@LogExecutionTime
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    private DeviceEquipmentService deviceEquipmentService;
    private DeviceService deviceService;

    @EJB
    public void setDeviceEquipmentService(DeviceEquipmentService deviceEquipmentService) {
        this.deviceEquipmentService = deviceEquipmentService;
    }

    @EJB
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE})
    public Response list(@QueryParam(NAME) String name,
                         @QueryParam(NAME_PATTERN) String namePattern,
                         @QueryParam(STATUS) String status,
                         @QueryParam(NETWORK_ID) Long networkId,
                         @QueryParam(NETWORK_NAME) String networkName,
                         @QueryParam(DEVICE_CLASS_ID) Long deviceClassId,
                         @QueryParam(DEVICE_CLASS_NAME) String deviceClassName,
                         @QueryParam(DEVICE_CLASS_VERSION) String deviceClassVersion,
                         @QueryParam(SORT_FIELD) String sortField,
                         @QueryParam(SORT_ORDER) @SortOrder Boolean sortOrder,
                         @QueryParam(TAKE) @Min(0) @Max(Integer.MAX_VALUE) Integer take,
                         @QueryParam(SKIP) @Min(0) @Max(Integer.MAX_VALUE) Integer skip) {

        logger.debug("Device list requested");

        if (sortOrder == null) {
            sortOrder = true;
        }
        if (sortField != null
                && !NAME.equalsIgnoreCase(sortField)
                && !STATUS.equalsIgnoreCase(sortField)
                && !NETWORK.equalsIgnoreCase(sortField)
                && !DEVICE_CLASS.equalsIgnoreCase(sortField)) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();

        List<Device> result = deviceService.getList(name, namePattern, status, networkId, networkName, deviceClassId,
                deviceClassName, deviceClassVersion, sortField, sortOrder, take, skip, principal);

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
    @AllowedKeyAction(action = {REGISTER_DEVICE})
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(JsonObject jsonObject, @PathParam(ID) String deviceGuid) {
        logger.debug("Device register method requested. Guid : {}", deviceGuid);

        Gson mainGson = GsonFactory.createGson(DEVICE_PUBLISHED);
        DeviceUpdate device;
        device = mainGson.fromJson(jsonObject, DeviceUpdate.class);
        device.setGuid(new NullableWrapper<>(deviceGuid));
        Gson gsonForEquipment = GsonFactory.createGson();
        boolean useExistingEquipment = jsonObject.get(EQUIPMENT) == null;
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(
                jsonObject.get(EQUIPMENT),
                new TypeToken<HashSet<Equipment>>() {
                }.getType());

        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        deviceService.deviceSaveAndNotify(device, equipmentSet, ThreadLocalVariablesKeeper.getPrincipal(),
                useExistingEquipment);
        logger.debug("Device register finished successfully. Guid : {}", deviceGuid);

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
    @AllowedKeyAction(action = {GET_DEVICE})
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    public Response get(@PathParam(ID) String guid) {
        logger.debug("Device get requested. Guid {}", guid);

        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();

        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        if (principal.getRole().equals(HiveRoles.DEVICE)) {
            logger.debug("Device get proceed successfully. Guid {}", guid);
            return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED_DEVICE_AUTH);
        }
        logger.debug("Device get proceed successfully. Guid {}", guid);
        return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED);
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
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response delete(@PathParam(ID) String guid) {

        logger.debug("Device delete requested");

        deviceService.deleteDevice(guid, ThreadLocalVariablesKeeper.getPrincipal());

        logger.debug("Device with id = {} is deleted", guid);

        return ResponseFactory.response(NO_CONTENT);
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
    @AllowedKeyAction(action = {GET_DEVICE_STATE})
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    public Response equipment(@PathParam(ID) String guid) {
        logger.debug("Device equipment requested for device {}", guid);

        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);
        List<DeviceEquipment> equipments = deviceEquipmentService.findByFK(device);

        logger.debug("Device equipment request proceed successfully for device {}", guid);

        return ResponseFactory.response(OK, equipments, DEVICE_EQUIPMENT_SUBMITTED);
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
    @AllowedKeyAction(action = {GET_DEVICE_STATE})
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    public Response equipmentByCode(@PathParam(ID) String guid,
                                    @PathParam(CODE) String code) {

        logger.debug("Device equipment by code requested");
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        DeviceEquipment equipment = deviceEquipmentService.findByCodeAndDevice(code, device);
        if (equipment == null) {
            logger.debug("No device equipment found for code : {} and guid : {}", code, guid);
            return ResponseFactory
                    .response(NOT_FOUND,
                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                    String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        logger.debug("Device equipment by code proceed successfully");

        return ResponseFactory.response(OK, equipment, DEVICE_EQUIPMENT_SUBMITTED);
    }


}
