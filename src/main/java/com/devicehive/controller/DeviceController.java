package com.devicehive.controller;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_SUBMITTED;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.Equipment;
import com.devicehive.service.DeviceService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * TODO JavaDoc
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
    private DeviceEquipmentDAO equipmentDAO;

    @GET
    @RolesAllowed({"Client", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(DEVICE_PUBLISHED)
    public List<Device> list(@QueryParam("name") String name,
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
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Name".equals(sortField) && !"Status".equals(sortField) && !"Network".equals(sortField) &&
                !"DeviceClass".equals(sortField) && sortField != null) {
            throw new HiveException("The sort field cannot be equal " + sortField);//maybe better to do sort field null
        }
        return deviceDAO.getList(name, namePattern, status, networkId, networkName, deviceClassId, deviceClassName,
                deviceClassVersion, sortField, sortOrderAsc, take, skip);
    }

    @PUT
    @Path("/{id}")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICE_SUBMITTED)
    public Response register(JsonObject jsonObject, @PathParam("id") String guid) {
        UUID deviceGuid = parseUUID(guid);
        Gson mainGson = GsonFactory.createGson(DEVICE_SUBMITTED);
        Device device = mainGson.fromJson(jsonObject, Device.class);
        device.setGuid(deviceGuid);
        deviceService.checkDevice(device);
        Gson gsonForEquipment = GsonFactory.createGson();
        Set<Equipment> equipmentSet =
                gsonForEquipment.fromJson(jsonObject.get("equipment"), new TypeToken<HashSet<Equipment>>() {
                }.getType());
        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        device.setGuid(deviceGuid);
        deviceService.deviceSave(device, equipmentSet);
        return Response.ok().build();
    }

    @GET
    @Path("/{id}")
//    @RolesAllowed({"Device", "Client", "Administrator"})
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(DEVICE_PUBLISHED)
    public Device get(@PathParam("id") String guid) {
        UUID deviceGuid = parseUUID(guid);
        Device device = deviceDAO.findByUUID(deviceGuid);
        if (device == null) {
            //TODO throw smth
            throw new HiveException("device with guid " + guid + " not found");
        }
        return device;
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("Administrator")
    public Response delete(@PathParam("id") String guid) {
        UUID deviceGuid = parseUUID(guid);
        Device device = deviceDAO.findByUUID(deviceGuid);
        if (device == null) {
            return Response.status(404).build();
        }
        commandDAO.deleteByFK(device);
        deviceDAO.deleteDevice(device.getId());
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/equipment")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(DEVICE_EQUIPMENT_SUBMITTED)
    public List<DeviceEquipment> equipment(@PathParam("id") String guid) {
        UUID deviceId = parseUUID(guid);
        Device device = deviceDAO.findByUUID(deviceId);
        if (device == null) {
            //TODO throw smth
            throw new HiveException("device with guid " + guid + " not found");
        }
        return equipmentDAO.findByFK(device);
    }

    private UUID parseUUID(String uuid) {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            //TODO throw smth
            throw new HiveException("unparseable guid: " + uuid);
        }
        return deviceId;
    }
}
