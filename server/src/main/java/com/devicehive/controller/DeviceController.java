package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.SortOrderQueryParamParser;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceEquipmentService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.LogExecutionTime;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.devicehive.auth.AllowedKeyAction.Action.*;
import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST controller for devices: <i>/device</i>. See <a href="http://www.devicehive.com/restful#Reference/Device">DeviceHive
 * RESTful API: Device</a> for details.
 */
@Path("/device")
@LogExecutionTime
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    @EJB
    private DeviceEquipmentService deviceEquipmentService;

    @EJB
    private DeviceService deviceService;

    @Inject
    private HiveSecurityContext hiveSecurityContext;


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
     * @param sortOrderSt        Result list sort order. Available values are ASC and DESC.
     * @param take               Number of records to take from the result list.
     * @param skip               Number of records to skip from the result list.
     * @return list of <a href="http://www.devicehive.com/restful#Reference/Device">Devices</a>
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE)
    public Response list(@QueryParam(NAME) String name,
                         @QueryParam(NAME_PATTERN) String namePattern,
                         @QueryParam(STATUS) String status,
                         @QueryParam(NETWORK_ID) Long networkId,
                         @QueryParam(NETWORK_NAME) String networkName,
                         @QueryParam(DEVICE_CLASS_ID) Long deviceClassId,
                         @QueryParam(DEVICE_CLASS_NAME) String deviceClassName,
                         @QueryParam(DEVICE_CLASS_VERSION) String deviceClassVersion,
                         @QueryParam(SORT_FIELD) String sortField,
                         @QueryParam(SORT_ORDER) String sortOrderSt,
                         @QueryParam(TAKE) @Min(0) @Max(Integer.MAX_VALUE) Integer take,
                         @QueryParam(SKIP) @Min(0) @Max(Integer.MAX_VALUE) Integer skip) {

        logger.debug("Device list requested");

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);
        if (sortField != null
            && !NAME.equalsIgnoreCase(sortField)
            && !STATUS.equalsIgnoreCase(sortField)
            && !NETWORK.equalsIgnoreCase(sortField)
            && !DEVICE_CLASS.equalsIgnoreCase(sortField)) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        List<Device> result = deviceService.getList(name, namePattern, status, networkId, networkName, deviceClassId,
                                                    deviceClassName, deviceClassVersion, sortField, sortOrder, take,
                                                    skip, principal);

        logger.debug("Device list proceed result. Result list contains {} elems", result.size());

        return ResponseFactory.response(Response.Status.OK, ImmutableSet.copyOf(result), JsonPolicyDef.Policy.DEVICE_PUBLISHED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/register">DeviceHive RESTful API:
     * Device: register</a> Registers a device. If device with specified identifier has already been registered, it gets
     * updated in case when valid key is provided in the authorization header.
     *
     * @param jsonObject In the request body, supply a Device resource. See <a href="http://www.devicehive
     *                   .com/restful#Reference/Device/register">
     * @param deviceGuid Device unique identifier.
     * @return response code 201, if successful
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @AllowedKeyAction(action = REGISTER_DEVICE)
    @PermitAll
    public Response register(JsonObject jsonObject, @PathParam(ID) String deviceGuid) {
        logger.debug("Device register method requested. Guid : {}", deviceGuid);

        Gson mainGson = GsonFactory.createGson(DEVICE_SUBMITTED);
        DeviceUpdate device = mainGson.fromJson(jsonObject, DeviceUpdate.class);
        device.setGuid(new NullableWrapper<>(deviceGuid));
        Gson gsonForEquipment = GsonFactory.createGson();
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(
            jsonObject.get(EQUIPMENT),
            new TypeToken<HashSet<Equipment>>() {
            }.getType());

        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        deviceService.deviceSaveAndNotify(device, equipmentSet, principal);
        logger.debug("Device register finished successfully. Guid : {}", deviceGuid);

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/get">DeviceHive RESTful API:
     * Device: get</a> Gets information about device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/Device">Device</a>
     *         resource in the response body.
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE)
    public Response get(@PathParam(ID) String guid) {
        logger.debug("Device get requested. Guid {}", guid);

        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();

        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        if (principal.getRole().equals(HiveRoles.DEVICE)) {
            logger.debug("Device get proceed successfully. Guid {}", guid);
            return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED_DEVICE_AUTH);
        }
        logger.debug("Device get proceed successfully. Guid {}", guid);
        return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/delete">DeviceHive RESTful API:
     * Device: delete</a> Deletes an existing device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns an empty response body.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY, HiveRoles.DEVICE})
    @AllowedKeyAction(action = REGISTER_DEVICE)
    public Response delete(@PathParam(ID) String guid) {

        logger.debug("Device delete requested");
        final Device device = deviceService.findByGuidWithPermissionsCheck(guid, hiveSecurityContext.getHivePrincipal());
        if (device == null || !guid.equals(device.getGuid())) {
            logger.debug("No device found for guid : {}", guid);
            return ResponseFactory
                    .response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                                    String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        deviceService.deleteDevice(guid, hiveSecurityContext.getHivePrincipal());

        logger.debug("Device with id = {} is deleted", guid);

        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/equipment">DeviceHive RESTful API:
     * Device: equipment</a> Gets current state of device equipment. The equipment state is tracked by framework and it
     * could be updated by sending 'equipment' notification with the following parameters: equipment: equipment code
     * parameters: current equipment state
     *
     * @param guid Device unique identifier.
     * @return If successful, this method returns array of the following structures in the response body. <table> <tr>
     *         <td>Property Name</td> <td>Type</td> <td>Description</td> </tr> <tr> <td>id</td> <td>string</td>
     *         <td>Equipment code.</td> </tr> <tr> <td>timestamp</td> <td>datetime</td> <td>Equipment state
     *         timestamp.</td> </tr> <tr> <td>parameters</td> <td>object</td> <td>Current equipment state.</td> </tr>
     *         </table>
     */
    @GET
    @Path("/{id}/equipment")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_STATE)
    public Response equipment(@PathParam(ID) String guid) {
        logger.debug("Device equipment requested for device {}", guid);

        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);
        List<DeviceEquipment> equipments = deviceEquipmentService.findByFK(device);

        logger.debug("Device equipment request proceed successfully for device {}", guid);

        return ResponseFactory.response(OK, equipments, DEVICE_EQUIPMENT_SUBMITTED);
    }

    /**
     * Gets current state of device equipment. The equipment state is tracked by framework and it could be updated by
     * sending 'equipment' notification with the following parameters: equipment: equipment code parameters: current
     * equipment state
     *
     * @param guid device guid
     * @param code equipment code
     * @return If successful return equipment associated with code and device with following guid
     */
    @GET
    @Path("/{id}/equipment/{code}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_STATE)
    public Response equipmentByCode(@PathParam(ID) String guid,
                                    @PathParam(CODE) String code) {

        logger.debug("Device equipment by code requested");
        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
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
