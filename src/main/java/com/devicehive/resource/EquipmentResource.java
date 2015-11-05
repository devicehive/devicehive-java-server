package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.EquipmentUpdate;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = {"DeviceClass"})
@Path("/device/class/{deviceClassId}/equipment")
public interface EquipmentResource {

    /**
     * Gets current state of device equipment. <code> [ { "id":1, "timestamp": "1970-01-01 00:00:00.0", "parameters":{/
     * *custom json object* /} }, { "id":2, "timestamp": "1970-01-01 00:00:00.0", "parameters":{/ *custom json object*
     * /} } ] <p/> </code>
     *
     * @param classId device class id
     * @param eqId    equipment id
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @ApiOperation(value = "Get equipment", notes = "Returns equipment by device class id and equipment id")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If equipment not found")
    })
    Response getEquipment(
            @ApiParam(name = "deviceClassId", value = "Device class id", required = true)
            @PathParam("deviceClassId")
            long classId,
            @ApiParam(name = "id", value = "Equipment id", required = true)
            @PathParam("id")
            long eqId);

    /**
     * Adds new equipment type to device class
     *
     * @param classId device class id
     */
    @POST
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create equipment", notes = "Creates equipment")
    Response insertEquipment(
            @ApiParam(name = "deviceClassId", value = "Device class id", required = true)
            @PathParam("deviceClassId")
            long classId,
            @ApiParam(value = "Equipment body", required = true, defaultValue = "{}")
            Equipment equipment);

    /**
     * Updates device class' equipment. None of following parameters are mandatory. Parameters, if left unspecified,
     * remains unchanged, instead setting parameter to null will null corresponding value. In following JSON <p/> name
     * 	Equipment display name. code 	Equipment code. It's used to reference particular equipment and it should be
     * unique within a device class. type 	Equipment type. An arbitrary string representing equipment capabilities. data
     * 	Equipment data, a JSON object with an arbitrary structure. <p/> <code> { "name": "equipment name", "code":
     * "equipment_code", "type": "equipment_type", "data": {/ * json object* /} } </code>
     *
     * @param classId         id of class
     * @param eqId            equipment id
     * @param equipmentUpdate Json  object
     * @return empty response with status 201 in case of success, empty response with status 404, if there's no such
     *         record
     */
    @PUT
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update equipment", notes = "Updates equipment")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If equipment not found")
    })
    Response updateEquipment(
            @ApiParam(name = "deviceClassId", value = "Device class id", required = true)
            @PathParam("deviceClassId")
            long classId,
            @ApiParam(name = "id", value = "Equipment id", required = true)
            @PathParam("id")
            long eqId,
            @ApiParam(value = "Equipment body", required = true, defaultValue = "{}")
            @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENT_PUBLISHED)
            EquipmentUpdate equipmentUpdate);

    /**
     * Will cascade deletes specified equipment and all data for this equipment for all devise of this type.
     *
     * @param classId Device class id
     * @param eqId    Equipment id
     * @return empty body, 204 if success, 404 if no record found
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_DEVICE_CLASS')")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete equipment", notes = "Deletes equipment")
    Response deleteEquipment(
            @ApiParam(name = "deviceClassId", value = "Device class id", required = true)
            @PathParam("deviceClassId")
            long classId,
            @ApiParam(name = "id", value = "Equipment id", required = true)
            @PathParam("id")
            long eqId);
}
