package com.devicehive.resource;

import com.google.gson.JsonObject;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;

/**
 * REST controller for devices: <i>/device</i>. See <a href="http://www.devicehive.com/restful#Reference/Device">DeviceHive
 * RESTful API: Device</a> for details.
 */
@Path("/device")
public interface DeviceResource {

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
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE')")
    Response list(
            @QueryParam(NAME) String name,
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
            @QueryParam(SKIP) @Min(0) @Max(Integer.MAX_VALUE) Integer skip);

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
    @PreAuthorize("hasPermission(null, 'REGISTER_DEVICE')")
    Response register(
            JsonObject jsonObject,
            @PathParam(ID) String deviceGuid);

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
    @PreAuthorize("hasAnyRole('CLIENT', 'DEVICE', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE')")
    Response get(
            @PathParam(ID) String guid);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/Device/delete">DeviceHive RESTful API:
     * Device: delete</a> Deletes an existing device.
     *
     * @param guid Device unique identifier
     * @return If successful, this method returns an empty response body.
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY', 'DEVICE') and hasPermission(null, 'REGISTER_DEVICE')")
    Response delete(
            @PathParam(ID) String guid);

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
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_STATE')")
    Response equipment(
            @PathParam(ID) String guid);

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
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_DEVICE_STATE')")
    Response equipmentByCode(
            @PathParam(ID) String guid,
            @PathParam(CODE) String code);
}
