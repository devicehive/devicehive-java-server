package com.devicehive.controller;

import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.Equipment;
import com.devicehive.service.DeviceService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_SUBMITTED;

/**
 * TODO JavaDoc
 */

@Path("/device")
public class DeviceController {

    @Inject
    private DeviceService deviceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
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

        return Response.ok().build();
    }

    @PUT
    @Path("/{id}")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.DEVICE_SUBMITTED)
    public Response register(JsonObject jsonObject, @PathParam("id") String guid) {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            return Response.status(400).build();
        }
        Gson mainGson = GsonFactory.createGson(DEVICE_SUBMITTED);
        Device device = mainGson.fromJson(jsonObject, Device.class);
        device.setGuid(deviceId);
        deviceService.checkDevice(device);
        Gson gsonForEquipment = GsonFactory.createGson();
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(jsonObject.get("equipment"),new TypeToken<HashSet<Equipment>>() {
                }.getType());
        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        device.setGuid(deviceId);
        deviceService.deviceSave(device, equipmentSet);
        return Response.ok().build();
    }

    public Response getDeviceList() {
        return Response.ok().build();
    }
}
