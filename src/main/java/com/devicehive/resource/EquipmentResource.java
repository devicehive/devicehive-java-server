package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.EquipmentUpdate;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.DEVICE_CLASS_ID;
import static com.devicehive.configuration.Constants.ID;

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
    @PreAuthorize("hasRole('ADMIN')")
    Response getEquipment(
            @PathParam(DEVICE_CLASS_ID) long classId,
            @PathParam(ID) long eqId);

    /**
     * Adds new equipment type to device class
     *
     * @param classId device class id
     */
    @POST
    @PreAuthorize("hasRole('ADMIN')")
    @Consumes(MediaType.APPLICATION_JSON)
    Response insertEquipment(
            @PathParam(DEVICE_CLASS_ID) long classId,
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
    @PreAuthorize("hasRole('ADMIN')")
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateEquipment(
            @PathParam(DEVICE_CLASS_ID) long classId,
            @PathParam(ID) long eqId,
            @JsonPolicyApply(JsonPolicyDef.Policy.EQUIPMENT_PUBLISHED) EquipmentUpdate equipmentUpdate);

    /**
     * Will cascade deletes specified equipment and all data for this equipment for all devise of this type.
     *
     * @param classId Device class id
     * @param eqId    Equipment id
     * @return empty body, 204 if success, 404 if no record found
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Consumes(MediaType.APPLICATION_JSON)
    Response deleteEquipment(
            @PathParam(DEVICE_CLASS_ID) long classId,
            @PathParam(ID) long eqId);

    /**
     * Gets current state of device equipment. <code> [ { "id":1, "timestamp": "1970-01-01 00:00:00.0", "parameters":{/
     * *custom json object* /} }, { "id":2, "timestamp": "1970-01-01 00:00:00.0", "parameters":{/ *custom json object*
     * /} } ] <p/> </code>
     */
    @GET
    @PreAuthorize("hasRole('ADMIN')")
    Response getEquipment();
}
