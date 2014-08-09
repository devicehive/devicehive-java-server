package com.devicehive.messaging;

import com.devicehive.client.HiveClient;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by stas on 09.08.14.
 */
public class AdminTool {


    private final static String DEVICE_NAME = "MessagingTestDevice";
    private final static String DEVICE_CLASS = "MessagingTestDeviceClass";
    private final static String NETWORK = "MessagingTestNetwork";
    private final static String USER = "MessagingTestUser";

    private HiveClient adminClient;


    public AdminTool(HiveClient adminClient) {
        this.adminClient = adminClient;
    }


    private void cleanUser() throws HiveException {
        List<User> users =  adminClient.getUserController().listUsers(USER, null, null, null, null, null, null, null);
        for (User user : users) {
            user.setStatus(UserStatus.LOCKED_OUT);
            adminClient.getUserController().updateUser(user);
        }
    }

    private User findUser() throws HiveException {
        List<User> users = adminClient.getUserController().listUsers(USER, null, null, null, null, null, null, null);
        User user = null;
        if (users.isEmpty()) {
            user = new User();
            user.setLogin(USER);
            user.setRole(UserRole.ADMIN);
            user.setPassword(UUID.randomUUID().toString());
            user.setStatus(UserStatus.ACTIVE);
            user = adminClient.getUserController().insertUser(user);
            user = adminClient.getUserController().getUser(user.getId());
        } else {
            user = users.iterator().next();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            adminClient.getUserController().updateUser(user);
        }
        return user;
    }

    private void cleanKeys() throws HiveException {
        List<AccessKey> keyList = adminClient.getAccessKeyController().listKeys();
        for (AccessKey key : keyList) {
            adminClient.getAccessKeyController().deleteKey(key.getId());
        }
    }

    private void cleanTestDevices() throws HiveException {
        List<Network> list = adminClient.getNetworkController().listNetworks(NETWORK, null, null, null, null, null);
        for (Network network : list) {
            List<Device> devices = adminClient.getDeviceController().listDevices(null, null, null, network.getId(), null, null, null, null, null, null, null, null);
            for (Device device : devices) {
                adminClient.getDeviceController().deleteDevice(device.getId());
            }
        }
    }

    public List<Device> prepareTestDevices(int deviceCount) throws HiveException {
        DeviceClass deviceClass = registerDeviceClass();
        Network network = registerNetwork();
        return prepareTestDevices(deviceCount, deviceClass, network);
    }

    private List<Device> prepareTestDevices(int deviceCount, DeviceClass deviceClass, Network network) throws HiveException {
        for (int i = 0; i < deviceCount; i++) {
            Device device = new Device();
            device.setId(DEVICE_NAME + i);
            device.setName(DEVICE_NAME + i);
            device.setDeviceClass(deviceClass);
            device.setNetwork(network);
            device.setKey(UUID.randomUUID().toString());
            adminClient.getDeviceController().registerDevice(device.getId(), device);
        }
        return adminClient.getDeviceController().listDevices(null, null, null, network.getId(), null, null, null, null, null, null, null, null);
    }


    private void cleanDeviceClass() throws HiveException {
        List<DeviceClass> list = adminClient.getDeviceController().listDeviceClass(DEVICE_CLASS, null, null, null, null, null, null);
        for (DeviceClass deviceClass : list) {
            adminClient.getDeviceController().deleteDeviceClass(deviceClass.getId());
        }
    }

    private DeviceClass registerDeviceClass() throws HiveException {
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setPermanent(true);
        deviceClass.setName(DEVICE_CLASS);
        deviceClass.setVersion("1");
        deviceClass.setOfflineTimeout(60);
        long id = adminClient.getDeviceController().insertDeviceClass(deviceClass);
        return adminClient.getDeviceController().getDeviceClass(id);
    }

    private void cleanNetwork() throws HiveException {
        User user = findUser();
        List<Network> list = adminClient.getNetworkController().listNetworks(NETWORK, null, null, null, null, null);
        for (Network network : list) {
            adminClient.getUserController().unassignNetwork(user.getId(), network.getId());
            adminClient.getNetworkController().deleteNetwork(network.getId());
        }
    }

    private Network registerNetwork() throws HiveException {
        Network network = new Network();
        network.setName(NETWORK);
        network.setDescription(NETWORK);
        long id = adminClient.getNetworkController().insertNetwork(network);
        User user = findUser();
        adminClient.getUserController().assignNetwork(user.getId(), id);
        return adminClient.getNetworkController().getNetwork(id);
    }


    public List<AccessKey> prepareKeys(List<Device> devices) throws HiveException {
        User user = findUser();
        List<AccessKey> res = new ArrayList<>();
        for (Device device: devices) {
            AccessKey key = createKey(device);
            key = adminClient.getAccessKeyController().insertKey(user.getId(), key);
            res.add(adminClient.getAccessKeyController().getKey(user.getId(), key.getId()));
        }
        return res;
    }


    private AccessKey createKey(Device device) {
        AccessKey accessKey = new AccessKey();
        accessKey.setKey(device.getId());
        accessKey.setLabel(device.getId());
        AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Sets.newHashSet(permission));
        permission.setActions(AccessKeyPermission.KNOWN_ACTIONS);
        permission.setDevices(Sets.newHashSet(device.getId()));
        return accessKey;
    }


    public void cleanup() throws HiveException {
        cleanKeys();
        cleanTestDevices();
        cleanNetwork();
        cleanDeviceClass();
        cleanUser();
    }

}
