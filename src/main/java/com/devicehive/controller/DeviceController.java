package com.devicehive.controller;

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

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

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
    @Context
    private ContainerRequestContext requestContext;

    @GET
    @RolesAllowed({"CLIENT", "ADMIN"})
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
        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            throw new BadRequestException("The sort order cannot be equal " + sortOrder);
        }
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Name".equals(sortField) && !"Status".equals(sortField) && !"Network".equals(sortField) &&
                !"DeviceClass".equals(sortField) && sortField != null) {
            throw new BadRequestException("The sort field cannot be equal " + sortField);
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
        UUID deviceGuid;
        try {
            deviceGuid = UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unparseable guid: " + guid);
        }
        if (jsonObject.get("key") == null) {
            //use existing
        }
        Gson mainGson = GsonFactory.createGson(DEVICE_SUBMITTED);
        Device device = mainGson.fromJson(jsonObject, Device.class);
        //todo no key
        device.setGuid(deviceGuid);
        try {
            deviceService.checkDevice(device);
        } catch (HiveException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
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
    @RolesAllowed({"Device", "CLIENT", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(DEVICE_PUBLISHED)
    public Device get(@PathParam("id") String guid) {
        return getDevice(guid);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") String guid) {
        Device device = getDevice(guid);
        commandDAO.deleteByFK(device);
        deviceDAO.deleteDevice(device.getId());
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/equipment")
    @RolesAllowed({"CLIENT", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(DEVICE_EQUIPMENT_SUBMITTED)
    public List<DeviceEquipment> equipment(@PathParam("id") String guid) {
        Device device = getDevice(guid);
        return equipmentDAO.findByFK(device);
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

    private boolean checkPermission() {
        return false;
    }

}
