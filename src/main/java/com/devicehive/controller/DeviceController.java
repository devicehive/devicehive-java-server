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
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.hash.HashMapBased;
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
import java.sql.Timestamp;
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
    private DeviceNotificationDAO notificationDAO;
    @Inject
    private DeviceEquipmentDAO equipmentDAO;
    @Inject
    @HashMapBased
    /* Supported implementations: DerbyBased and HashMapBased */
    private MessagesDataSource messagesDataSource;
    @Inject
    private DeviceEquipmentDAO deviceEquipmentDAO;

    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
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
        Gson mainGson = GsonFactory.createGson(DEVICE_SUBMITTED);
        DeviceUpdate device;
        device = mainGson.fromJson(jsonObject, DeviceUpdate.class);

        NullableWrapper<UUID> uuidNullableWrapper = new NullableWrapper<>();
        uuidNullableWrapper.setValue(deviceGuid);

        device.setGuid(uuidNullableWrapper);
        try {
            deviceService.checkDevice(device);
        } catch (HiveException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
        Gson gsonForEquipment = GsonFactory.createGson();
        boolean useExistingEquipment = jsonObject.get("equipment") == null;
        Set<Equipment> equipmentSet =
                gsonForEquipment.fromJson(jsonObject.get("equipment"), new TypeToken<HashSet<Equipment>>() {
                }.getType());
        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        deviceService.deviceSave(device, equipmentSet, useExistingEquipment);
        return Response.status(201).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(DEVICE_PUBLISHED)
    public Device get(@PathParam("id") String guid) {
        return getDevice(guid);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(HiveRoles.ADMIN)
    public Response delete(@PathParam("id") String guid) {
        Device device = getDevice(guid);
        List<DeviceCommand> commandList = commandDAO.getNewerThan(device, new Timestamp(0));
        notificationDAO.deleteNotificationByFK(device);
        messagesDataSource.removeCommandsSubscription(device.getId());
        for (DeviceCommand command : commandList) {
            messagesDataSource.removeCommandsUpdatesSubscription(command.getId());
        }
        commandDAO.deleteByFK(device);
        deviceEquipmentDAO.deleteByFK(device);
        if (!deviceDAO.deleteDevice(device.getId())) {
            throw new NotFoundException("Device with id = " + guid + " not found");
        }
        return Response.status(204).build();
    }

    @GET
    @Path("/{id}/equipment")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
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
