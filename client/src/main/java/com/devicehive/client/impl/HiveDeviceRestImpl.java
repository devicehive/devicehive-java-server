package com.devicehive.client.impl;


import com.devicehive.client.HiveDevice;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.util.HiveValidator;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;


public class HiveDeviceRestImpl implements HiveDevice {

    protected HiveContext hiveContext;

    public HiveDeviceRestImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    /**
     * Closes single hive device with associated context
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    @Override
    public void authenticate(String deviceId, String deviceKey) throws HiveException {
        hiveContext.setHivePrincipal(HivePrincipal.createDevice(deviceId, deviceKey));
    }


    @Override
    public Device getDevice() throws HiveException {
        String deviceId = hiveContext.getHivePrincipal().getDevice().getKey();
        if (deviceId == null) {
            throw new HiveClientException("Device is not authenticated");
        }
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, Device.class, null);
    }


    @Override
    public void registerDevice(Device device) throws HiveException {
        HiveValidator.validate(device);
        String path = "/device/" + device.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, device, null);
    }

    @SuppressWarnings("serial")
    @Override
    public List<DeviceCommand> queryCommands(Timestamp start, Timestamp end, String command, String status,
                                             String sortBy, boolean sortAsc, Integer take, Integer skip)
            throws HiveException {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("command", command);
        queryParams.put("status", status);
        queryParams.put("sortField", sortBy);
        String order = sortAsc ? "ASC" : "DESC";
        queryParams.put("sortOrder", order);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), null);
    }


    @Override
    public DeviceCommand getCommand(long commandId) throws HiveException {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, DeviceCommand.class, null);
    }


    @Override
    public void updateCommand(DeviceCommand deviceCommand) throws HiveException {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + deviceCommand.getId();
        hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.PUT, null, deviceCommand, COMMAND_UPDATE_FROM_DEVICE);
    }


    @Override
    public void subscribeForCommands(final Timestamp timestamp) throws HiveException {
        SubscriptionFilter filter = SubscriptionFilter.createForDevice(hiveContext.getHivePrincipal().getDevice().getLeft(), timestamp);
        hiveContext.getRestSubManager().addCommandsSubscription(filter);
    }

    /**
     * Unsubscribes the device from commands.
     */
    @Override
    public void unsubscribeFromCommands() throws HiveException {
        hiveContext.getRestSubManager().removeCommandSubscription();
    }


    @Override
    public DeviceNotification insertNotification(DeviceNotification deviceNotification) throws HiveException {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        HiveValidator.validate(deviceNotification);
        String path = "/device/" + authenticated.getKey() + "/notification";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, deviceNotification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
    }

    @Override
    public ApiInfo getInfo() throws HiveException {
        return hiveContext.getInfo();
    }

}
