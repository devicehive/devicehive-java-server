package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static java.util.Collections.singleton;
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
    @Autowired
    private AccessKeyService accessKeyService;

    private final Set<DeviceClassEquipmentVO> emptyEquipmentSet = Collections.<DeviceClassEquipmentVO>emptySet();

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

        deviceService.deviceSaveAndNotify(deviceUpdate, emptyEquipmentSet, principal);
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

        NetworkVO network = DeviceFixture.createNetwork();
        network = networkService.create(network);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");
        userService.assignNetwork(user.getId(), network.getId());
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, emptyEquipmentSet, principal);

        final DeviceNotification existingNotification = deviceNotificationService.find(null, device.getGuid());

        assertNotNull(existingNotification);
        assertEquals(device.getGuid(), existingNotification.getDeviceGuid());

        final DeviceVO existingDevice = deviceService.getDeviceWithNetworkAndDeviceClass(device.getGuid(), principal);
        assertNotNull(existingDevice);
        assertEquals(device.getGuid(), existingDevice.getGuid());
        assertEquals(dc.getName().orElse(null), existingDevice.getDeviceClass().getName());
    }

    /**
     * Test amdin can't create device without network when admin user hasn't networks. Admin authorized with basic auth.
     */
    @Test
    public void should_throw_HiveException_when_role_admin_without_networks_and_basic_authorized_create_device_without_network() {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.NO_NETWORKS_ASSIGNED_TO_USER);

        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, emptyEquipmentSet, principal);
    }

    /**
     * Test amdin can't create device without network when admin user hasn't networks. Admin authorized with key.
     */
    @Test
    public void should_throw_HiveException_when_role_admin_without_networks_and_key_authorized_create_device_without_network() {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.NO_NETWORKS_ASSIGNED_TO_USER);

        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        AccessKey accessKey = new AccessKey();
        accessKey.setKey(RandomStringUtils.randomAlphabetic(10));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setType(AccessKeyType.SESSION);
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setActionsArray(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue());
        permission.setDeviceGuidsCollection(Arrays.asList("1", "2", "3"));
        permission.setDomainArray("domain1", "domain2");
        permission.setNetworkIdsCollection(Arrays.asList(1L, 2L));
        permission.setSubnetsArray("localhost");
        accessKey.setPermissions(singleton(permission));
        AccessKey createdkey = accessKeyService.create(user, accessKey);

        final HivePrincipal principal = new HivePrincipal(createdkey);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, emptyEquipmentSet, principal);
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

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(Network.convert(network)));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        final HivePrincipal principal = new HivePrincipal(accessKey);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSaveAndNotify(deviceUpdate, emptyEquipmentSet, principal);
        final DeviceVO existingDevice = deviceService.getDeviceWithNetworkAndDeviceClass(device.getGuid(), principal);
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

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate0.setNetwork(Optional.ofNullable(Network.convert(network)));
        deviceUpdate1.setNetwork(Optional.ofNullable(Network.convert(network)));
        deviceUpdate2.setNetwork(Optional.ofNullable(Network.convert(network)));

        deviceService.deviceSave(deviceUpdate0, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate2, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.findByGuidWithPermissionsCheck(
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

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(Network.convert(network)));

        final NetworkVO network1 = new NetworkVO();
        network1.setName("" + randomUUID());
        NetworkVO created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetwork(Optional.ofNullable(Network.convert(network1)));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        final HivePrincipal principal = new HivePrincipal(accessKey);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.findByGuidWithPermissionsCheck(
                Arrays.asList(device.getGuid(), device1.getGuid()), principal);
        assertNotNull(devices);
        assertEquals(1, devices.size());
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

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(Network.convert(network)));

        final NetworkVO network1 = new NetworkVO();
        network1.setName("" + randomUUID());
        NetworkVO created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetwork(Optional.ofNullable(Network.convert(network1)));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);

        final DeviceVO existingDevice = deviceService.findByGuidWithPermissionsCheck(device.getGuid(), null);

        HivePrincipal principal = new HivePrincipal(existingDevice);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        final List<DeviceVO> devices = deviceService.findByGuidWithPermissionsCheck(
                Arrays.asList(device.getGuid(), device1.getGuid()), principal);
        assertNotNull(devices);
        assertEquals(1, devices.size());
        assertEquals(devices.get(0).getGuid(), device.getGuid());
    }

    @Test
    public void should_save_and_find_by_device_name() {
        final Device device = DeviceFixture.createDevice();
        String deviceName1 = RandomStringUtils.randomAlphabetic(10);
        device.setName(deviceName1);
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dc);

        final Device device1 = DeviceFixture.createDevice();
        String deviceName2 = RandomStringUtils.randomAlphabetic(10);
        device1.setName(deviceName2);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dc);

        final Device device2 = DeviceFixture.createDevice();
        String deviceName3 = RandomStringUtils.randomAlphabetic(10);
        device2.setName(deviceName3);
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2, dc);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate2, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.getList(deviceName1, null, null, null, null, null, null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(devices.size(), 1);
        assertEquals(device.getGuid(), devices.get(0).getGuid());
        assertEquals(device.getName(), devices.get(0).getName());
    }

    @Test
    public void should_save_and_find_by_device_status() {
        final Device device = DeviceFixture.createDevice();
        String status = RandomStringUtils.randomAlphabetic(10);
        device.setStatus(status);
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dc);

        String status1 = RandomStringUtils.randomAlphabetic(10);
        final Device device1 = DeviceFixture.createDevice();
        device1.setStatus(status1);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dc);

        final Device device2 = DeviceFixture.createDevice();
        device2.setStatus(status1);
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2, dc);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate2, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.getList(null, null, status1, null, null, null, null, null, false, null, null, null);
        Collections.sort(devices, (DeviceVO a, DeviceVO b) -> a.getId().compareTo(b.getId()));
        assertNotNull(devices);
        assertEquals(2, devices.size());
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

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetwork(Optional.ofNullable(Network.convert(network)));

        final NetworkVO network1 = new NetworkVO();
        network1.setName("" + randomUUID());
        NetworkVO created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetwork(Optional.ofNullable(Network.convert(network1)));

        final AccessKey accessKey = new AccessKey();
        final AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.getList(null, null, null, network1.getId(), null, null, null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device1.getGuid(), devices.get(0).getGuid());
        assertNotNull(devices.get(0).getNetwork());
        assertEquals(network1.getId(), devices.get(0).getNetwork().getId());
    }

    @Test
    public void should_save_and_find_by_device_class_id() {
        final Device device = DeviceFixture.createDevice();
        DeviceClassWithEquipmentVO dc = DeviceFixture.createDCVO();
        dc = deviceClassService.addDeviceClass(dc);
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(dc);
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dcUpdate);

        final Device device1 = DeviceFixture.createDevice();
        DeviceClassWithEquipmentVO dc1 = DeviceFixture.createDCVO();
        dc1 = deviceClassService.addDeviceClass(dc1);
        final DeviceClassUpdate dcUpdate1 = DeviceFixture.createDeviceClassUpdate(dc1);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dcUpdate1);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.getList(null, null, null, null, null, dc.getId(), null, null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device.getGuid(), devices.get(0).getGuid());
    }

    @Test
    public void should_save_and_find_by_device_class_name() {
        final Device device = DeviceFixture.createDevice();
        DeviceClassWithEquipmentVO dc = DeviceFixture.createDCVO();
        dc = deviceClassService.addDeviceClass(dc);
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(dc);
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device, dcUpdate);

        final Device device1 = DeviceFixture.createDevice();
        DeviceClassWithEquipmentVO dc1 = DeviceFixture.createDCVO();
        dc1 = deviceClassService.addDeviceClass(dc1);
        final DeviceClassUpdate dcUpdate1 = DeviceFixture.createDeviceClassUpdate(dc1);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1, dcUpdate1);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);

        final List<DeviceVO> devices = deviceService.getList(null, null, null, null, null, null, dc.getName(), null, false, null, null, null);
        assertNotNull(devices);
        assertEquals(device.getGuid(), devices.get(0).getGuid());
    }

    @Test
    public void should_delete_device() {
        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dcUpdate);

        deviceService.deviceSave(deviceUpdate, emptyEquipmentSet);
        DeviceVO existingDevice = deviceService.findByGuidWithPermissionsCheck(device.getGuid(), null);
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

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        Network nw = Network.convert(network);
        deviceUpdate0.setNetwork(Optional.ofNullable(nw));
        deviceUpdate1.setNetwork(Optional.ofNullable(nw));
        deviceUpdate2.setNetwork(Optional.ofNullable(nw));

        deviceService.deviceSave(deviceUpdate0, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate1, emptyEquipmentSet);
        deviceService.deviceSave(deviceUpdate2, emptyEquipmentSet);

        long count = deviceService.getAllowedDevicesCount(null, Arrays.asList(device0.getGuid(), device1.getGuid(), device2.getGuid()));
        assertEquals(3, count);
    }


    /**
     * Test checks that unauthorized user can't modify device
     */
    @Test
    public void should_throw_HiveException_when_user_is_unauthorized() throws Exception {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.UNAUTHORIZED_REASON_PHRASE);

        final Device device = DeviceFixture.createDevice();
        final DeviceClassUpdate dc = DeviceFixture.createDeviceClass();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getGuid(), dc);
             deviceService.deviceSave(deviceUpdate,emptyEquipmentSet);

        final HivePrincipal principal = new HivePrincipal();

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, emptyEquipmentSet, principal);
    }

}
