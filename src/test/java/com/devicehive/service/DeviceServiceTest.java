package com.devicehive.service;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

public class DeviceServiceTest extends AbstractResourceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceNotificationService deviceNotificationService;
    @Autowired
    private UserService userService;
    @Autowired
    private NetworkService networkService;
    @Autowired
    private DeviceClassService deviceClassService;

    /**
     * Test to check that device was successfully saved, notification send and retrieved back
     * using Client role.
     */
    @Test
    public void should_throw_HiveException_when_role_client_creates_device_without_network() throws Exception {
        expectedException.expect(HiveException.class);

        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, Collections.<Equipment>emptySet(), principal);
    }

    /**
     * Test to check that device was successfully saved, notification send and retrieved back
     * using Admin role.
     */
    @Test
    public void should_save_and_notify_role_admin() {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, Collections.<Equipment>emptySet(), principal);

        final DeviceNotification existingNotification = deviceNotificationService.find(null, device.getGuid());

        assertNotNull(existingNotification);
        assertEquals(device.getGuid(), existingNotification.getDeviceGuid());

        final Device existingDevice = deviceService.getDeviceWithNetworkAndDeviceClass(device.getGuid(), principal);
        assertNotNull(existingDevice);
        assertEquals(device.getGuid(), existingDevice.getGuid());
        assertEquals(device.getKey(), existingDevice.getKey());
        assertEquals(dc.getName().orElse(null), existingDevice.getDeviceClass().getName());
        assertEquals(dc.getVersion().orElse(null), existingDevice.getDeviceClass().getVersion());
    }

    /**
     * Test to check that device was successfully saved, notification send and retrieved back
     * using Key role.
     */
    @Test
    public void should_save_and_notify_role_key() throws UnknownHostException {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        final Network network = new Network();
        network.setName("" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(network));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        final HivePrincipal principal = new HivePrincipal(accessKey);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSaveAndNotify(deviceUpdate, Collections.<Equipment>emptySet(), principal);
        final Device existingDevice = deviceService.getDeviceWithNetworkAndDeviceClass(device.getGuid(), principal);
        assertNotNull(existingDevice);
    }

    /**
     * Test to check that devices were successfully saved and then all retrieved back
     * principal was null
     */
    @Test
    public void should_save_and_find_without_permissions() {
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();

        final Device device0 = DeviceFixture.createDevice();
        final DeviceUpdate deviceUpdate0 = DeviceFixture.createDevice(device0.getGuid(), dc);

        final Device device1 = DeviceFixture.createDevice();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getGuid(), dc);

        final Device device2 = DeviceFixture.createDevice();
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        final Network network = new Network();
        network.setName("" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate0.setNetwork(Optional.ofNullable(network));
        deviceUpdate1.setNetwork(Optional.ofNullable(network));
        deviceUpdate2.setNetwork(Optional.ofNullable(network));

        deviceService.deviceSave(deviceUpdate0, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate2, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.findByGuidWithPermissionsCheck(
                Arrays.asList(device0.getGuid(), device1.getGuid(), device2.getGuid()), null);
        assertNotNull(devices);
        assertEquals(devices.size(), 3);
    }

    /**
     * Test to check that devices were successfully saved and then user specific
     * device retrieved back
     */
    @Test
    public void should_save_and_find_by_user() throws UnknownHostException {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        final Device device1 = DeviceFixture.createDevice();
        final DeviceClassUpdate dc1 = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getGuid(), dc1);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        User user1 = new User();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1 = userService.createUser(user1, "123");

        final Network network = new Network();
        network.setName("" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(network));

        final Network network1 = new Network();
        network1.setName("" + randomUUID());
        Network created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetwork(Optional.ofNullable(network1));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        final HivePrincipal principal = new HivePrincipal(accessKey);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.findByGuidWithPermissionsCheck(
                Arrays.asList(device.getGuid(), device1.getGuid()), principal);
        assertNotNull(devices);
        assertEquals(devices.size(), 1);
        assertEquals(devices.get(0).getGuid(), device.getGuid());
    }

    /**
     * Test to check that devices were successfully saved and then device with specified id
     * retrieved back
     */
    @Test
    public void should_save_and_find_by_device_id() throws UnknownHostException {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        final Device device1 = DeviceFixture.createDevice();
        final DeviceClassUpdate dc1 = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getGuid(), dc1);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        User user1 = new User();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1 = userService.createUser(user1, "123");

        final Network network = new Network();
        network.setName("" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(network));

        final Network network1 = new Network();
        network1.setName("" + randomUUID());
        Network created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetwork(Optional.ofNullable(network1));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());

        final Device existingDevice = deviceService.findByGuidWithPermissionsCheck(device.getGuid(), null);

        final HivePrincipal principal = new HivePrincipal(existingDevice);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        final List<Device> devices = deviceService.findByGuidWithPermissionsCheck(
                Arrays.asList(device.getGuid(), device1.getGuid()), principal);
        assertNotNull(devices);
        assertEquals(devices.size(), 1);
        assertEquals(devices.get(0).getGuid(), device.getGuid());
    }

    @Test
    public void should_save_and_find_by_device_name() {
        final Device device = DeviceFixture.createDevice();
        device.setName("DEVICE_NAME");
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dc);

        final Device device1 = DeviceFixture.createDevice();
        device1.setName("DEVICE_NAME1");
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dc);

        final Device device2 = DeviceFixture.createDevice();
        device2.setName("DEVICE_NAME2");
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2, dc);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate2, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.getList("DEVICE_NAME", null, null, null, null, null, null, null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(devices.size(), 1);
        assertEquals(device.getGuid(), devices.get(0).getGuid());
        assertEquals(device.getName(), devices.get(0).getName());
    }

    @Test
    public void should_save_and_find_by_device_status() {
        final Device device = DeviceFixture.createDevice();
        device.setStatus("Online");
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dc);

        final Device device1 = DeviceFixture.createDevice();
        device1.setStatus("TEST");
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dc);

        final Device device2 = DeviceFixture.createDevice();
        device2.setStatus("TEST");
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2, dc);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate2, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.getList(null, null, "TEST", null, null, null, null, null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(devices.size(), 2);
        assertEquals(device1.getGuid(), devices.get(0).getGuid());
        assertEquals(device2.getGuid(), devices.get(1).getGuid());
    }

    @Test
    public void should_save_and_find_by_network_id() {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        final Device device1 = DeviceFixture.createDevice();
        final DeviceClassUpdate dc1 = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getGuid(), dc1);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        User user1 = new User();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1 = userService.createUser(user1, "123");

        final Network network = new Network();
        network.setName("" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(network));

        final Network network1 = new Network();
        network1.setName("" + randomUUID());
        Network created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetwork(Optional.ofNullable(network1));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.getList(null, null, null, network1.getId(), null, null, null, null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device1.getGuid(), devices.get(0).getGuid());
        assertNotNull(devices.get(0).getNetwork());
        assertEquals(network1.getId(), devices.get(0).getNetwork().getId());
    }

    @Test
    public void should_save_and_find_by_device_class_id() {
        final Device device = DeviceFixture.createDevice();
        DeviceClass dc = DeviceFixture.createDC();
        dc = deviceClassService.addDeviceClass(dc);
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(dc);
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dcUpdate);

        final Device device1 = DeviceFixture.createDevice();
        DeviceClass dc1 = DeviceFixture.createDC();
        dc1 = deviceClassService.addDeviceClass(dc1);
        final DeviceClassUpdate dcUpdate1 = DeviceFixture.createDeviceClassUpdate(dc1);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dcUpdate1);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.getList(null, null, null, null, null, dc.getId(), null, null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device.getGuid(), devices.get(0).getGuid());
    }

    @Test
    public void should_save_and_find_by_device_class_name() {
        final Device device = DeviceFixture.createDevice();
        DeviceClass dc = DeviceFixture.createDC();
        dc = deviceClassService.addDeviceClass(dc);
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(dc);
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dcUpdate);

        final Device device1 = DeviceFixture.createDevice();
        DeviceClass dc1 = DeviceFixture.createDC();
        dc1 = deviceClassService.addDeviceClass(dc1);
        final DeviceClassUpdate dcUpdate1 = DeviceFixture.createDeviceClassUpdate(dc1);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dcUpdate1);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.getList(null, null, null, null, null, null, dc.getName(), null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device.getGuid(), devices.get(0).getGuid());
    }

    @Test
    public void should_save_and_find_by_device_class_version() {
        final Device device = DeviceFixture.createDevice();
        DeviceClass dc = DeviceFixture.createDC();
        dc = deviceClassService.addDeviceClass(dc);
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(dc);
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dcUpdate);

        final Device device1 = DeviceFixture.createDevice();
        DeviceClass dc1 = DeviceFixture.createDC();
        dc1.setVersion("2");
        dc1 = deviceClassService.addDeviceClass(dc1);
        final DeviceClassUpdate dcUpdate1 = DeviceFixture.createDeviceClassUpdate(dc1);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dcUpdate1);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());

        final List<Device> devices = deviceService.getList(null, null, null, null, null, null, null, dc1.getVersion(), null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device1.getGuid(), devices.get(0).getGuid());
    }

    @Test
    public void should_delete_device() {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dcUpdate);

        deviceService.deviceSave(deviceUpdate, Collections.<Equipment>emptySet());
        Device existingDevice = deviceService.findByGuidWithPermissionsCheck(device.getGuid(), null);
        assertNotNull(existingDevice);

        deviceService.deleteDevice(device.getGuid(), null);
        existingDevice = deviceService.findByGuidWithPermissionsCheck(device.getGuid(), null);
        assertNull(existingDevice);
    }

    @Test
    public void should_return_device_count() {
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();

        final Device device0 = DeviceFixture.createDevice();
        final DeviceUpdate deviceUpdate0 = DeviceFixture.createDevice(device0.getGuid(), dc);

        final Device device1 = DeviceFixture.createDevice();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getGuid(), dc);

        final Device device2 = DeviceFixture.createDevice();
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        final Network network = new Network();
        network.setName("" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate0.setNetwork(Optional.ofNullable(network));
        deviceUpdate1.setNetwork(Optional.ofNullable(network));
        deviceUpdate2.setNetwork(Optional.ofNullable(network));

        deviceService.deviceSave(deviceUpdate0, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate1, Collections.<Equipment>emptySet());
        deviceService.deviceSave(deviceUpdate2, Collections.<Equipment>emptySet());

        long count = deviceService.getAllowedDevicesCount(null, Arrays.asList(device0.getGuid(), device1.getGuid(), device2.getGuid()));
        assertEquals(3, count);
    }
}
