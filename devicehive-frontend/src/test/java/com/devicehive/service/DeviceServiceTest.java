package com.devicehive.service;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListDeviceResponse;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeviceServiceTest extends AbstractResourceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private UserService userService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Autowired
    private DeviceDao deviceDao;

    @Mock
    private RequestHandler requestHandler;

    private ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    /**
     * Test to check that device was successfully saved, notification send and retrieved back
     * using Client role.
     */
    @Test
    public void should_throw_HiveException_when_role_client_creates_device_without_network() throws Exception {
    	expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NO_ACCESS_TO_NETWORK);

        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, VALID_PASSWORD);
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
    }

    /**
     * Test to check that device was successfully saved, notification send and retrieved back
     * using Admin role.
     */
    @Test
    public void should_save_and_notify_role_admin() throws Exception {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        NetworkVO network = DeviceFixture.createNetwork();
        network = networkService.create(network);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, VALID_PASSWORD);
        userService.assignNetwork(user.getId(), network.getId());
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        when(requestHandler.handle(Mockito.any(Request.class)))
                .thenAnswer(invocation -> {
                    Request request = (Request) invocation.getArguments()[0];
                    return Response.newBuilder()
                            .withCorrelationId(request.getCorrelationId())
                            .buildSuccess();
                });


        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
        TimeUnit.SECONDS.sleep(1);
        final DeviceVO existingDevice = deviceService.findById(device.getDeviceId());
        assertNotNull(existingDevice);
        assertEquals(device.getDeviceId(), existingDevice.getDeviceId());

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(requestHandler).handle(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        assertNotNull(request.getBody());
        assertTrue(request.getBody() instanceof NotificationInsertRequest);
        NotificationInsertRequest insertRequest = (NotificationInsertRequest) request.getBody();
        assertNotNull(insertRequest.getDeviceNotification());
        assertNotNull(insertRequest.getDeviceNotification().getId());
        assertNotNull(insertRequest.getDeviceNotification().getTimestamp());
        assertEquals(insertRequest.getDeviceNotification().getDeviceId(), existingDevice.getDeviceId());
        assertEquals(insertRequest.getDeviceNotification().getNotification(), SpecialNotifications.DEVICE_ADD);
    }

    /**
     * Test amdin can't create device without network when admin user hasn't networks. Admin authorized with token auth.
     */
    @Test
    public void should_throw_HiveException_when_role_admin_without_networks_and_token_authorized_create_device_without_network() {
        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NO_ACCESS_TO_NETWORK);

        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, VALID_PASSWORD);
        final HivePrincipal principal = new HivePrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
    }

    /**
     * Test amdin can't create device without network when admin user hasn't networks. Admin authorized with key.
     */
    @Test
    public void should_throw_HiveException_when_role_admin_without_networks_and_key_authorized_create_device_without_network() {
    	expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NO_ACCESS_TO_NETWORK);

        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, VALID_PASSWORD);

        final HivePrincipal principal = new HivePrincipal(user);
        principal.setNetworkIds(new HashSet<>(Arrays.asList(1L, 2L)));
        principal.setDeviceIds(new HashSet<>(Arrays.asList("1", "2", "3")));

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
    }

    /**
     * Test to check that device was successfully saved, notification send and retrieved back
     * using Key role.
     */
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_save_and_notify_role_key() throws Exception {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetworkId(created.getId());
        final HivePrincipal principal = new HivePrincipal(user);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        when(requestHandler.handle(Mockito.any(Request.class)))
                .thenAnswer(invocation -> {
                    Request request = (Request) invocation.getArguments()[0];
                    return Response.newBuilder()
                            .withCorrelationId(request.getCorrelationId())
                            .buildSuccess();
                });

        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
        TimeUnit.SECONDS.sleep(1);
        final DeviceVO existingDevice = deviceService.findById(device.getDeviceId());
        assertNotNull(existingDevice);
        assertEquals(existingDevice.getDeviceId(), deviceUpdate.getId().get());
        assertEquals(existingDevice.getName(), deviceUpdate.getName().get());

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(requestHandler).handle(requestCaptor.capture());

        Request request = requestCaptor.getValue();
        assertNotNull(request.getBody());
        assertTrue(request.getBody() instanceof NotificationInsertRequest);
        NotificationInsertRequest insertRequest = (NotificationInsertRequest) request.getBody();
        assertNotNull(insertRequest.getDeviceNotification());
        assertNotNull(insertRequest.getDeviceNotification().getId());
        assertNotNull(insertRequest.getDeviceNotification().getTimestamp());
        assertEquals(insertRequest.getDeviceNotification().getDeviceId(), existingDevice.getDeviceId());
        assertEquals(insertRequest.getDeviceNotification().getNotification(), SpecialNotifications.DEVICE_ADD);
    }

    /**
     * Test to check that devices were successfully saved and then all retrieved back
     * principal was null
     */
    @Test
    public void should_save_and_find_without_permissions() {
        final DeviceVO device0 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate0 = DeviceFixture.createDevice(device0.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        final DeviceVO device2 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate0.setNetworkId(network.getId());
        deviceUpdate1.setNetworkId(network.getId());
        deviceUpdate2.setNetworkId(network.getId());

        deviceService.deviceSave(deviceUpdate0);
        deviceService.deviceSave(deviceUpdate1);
        deviceService.deviceSave(deviceUpdate2);

        final List<DeviceVO> devices = deviceService.findByIdWithPermissionsCheck(
                Arrays.asList(device0.getDeviceId(), device1.getDeviceId(), device2.getDeviceId()), null);
        assertNotNull(devices);
        assertEquals(devices.size(), 3);
    }

    /**
     * Test to check that devices were successfully saved and then user specific
     * device retrieved back
     */
    @Test
    public void should_save_and_find_by_user() throws UnknownHostException {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);

        UserVO user1 = new UserVO();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1 = userService.createUser(user1, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetworkId(network.getId());

        final NetworkVO network1 = new NetworkVO();
        network1.setName("" + randomUUID());
        NetworkVO created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetworkId(network1.getId());
        
        final HivePrincipal principal = new HivePrincipal(user);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        deviceService.deviceSave(deviceUpdate);
        deviceService.deviceSave(deviceUpdate1);

        final List<DeviceVO> devices = deviceService.findByIdWithPermissionsCheck(
                Arrays.asList(device.getDeviceId(), device1.getDeviceId()), principal);
        assertNotNull(devices);
        assertEquals(1, devices.size());
        assertEquals(devices.get(0).getDeviceId(), device.getDeviceId());
    }

    /**
     * Test to check that devices were successfully saved and then device with specified id
     * retrieved back
     */
    @Test
    public void should_save_and_find_by_device_id() throws UnknownHostException {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);

        UserVO user1 = new UserVO();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1 = userService.createUser(user1, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetworkId(network.getId());

        final NetworkVO network1 = new NetworkVO();
        network1.setName("" + randomUUID());
        NetworkVO created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetworkId(network1.getId());

//        final AccessKey accessKey = new AccessKey();
//        final AccessKeyPermission permission = new AccessKeyPermission();
//        accessKey.setPermissions(Collections.singleton(permission));
//        accessKey.setUser(user);

        deviceService.deviceSave(deviceUpdate);
        deviceService.deviceSave(deviceUpdate1);
        
        Set<String> allowedDeviceIds = new HashSet<>();
        allowedDeviceIds.add(device.getDeviceId());

        HivePrincipal principal = new HivePrincipal();
        principal.setDeviceIds(allowedDeviceIds);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        final List<DeviceVO> devices = deviceService.findByIdWithPermissionsCheck(
                Arrays.asList(device.getDeviceId(), device1.getDeviceId()), principal);
        assertNotNull(devices);
        assertEquals(1, devices.size());
        assertEquals(devices.get(0).getDeviceId(), device.getDeviceId());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_save_and_find_by_device_name() throws Exception {
    	final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);

        final DeviceVO device = DeviceFixture.createDeviceVO();
        String deviceName1 = RandomStringUtils.randomAlphabetic(10);
        device.setName(deviceName1);
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device);
        deviceUpdate.setNetworkId(created.getId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        String deviceName2 = RandomStringUtils.randomAlphabetic(10);
        device1.setName(deviceName2);
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1);
        deviceUpdate1.setNetworkId(created.getId());

        final DeviceVO device2 = DeviceFixture.createDeviceVO();
        String deviceName3 = RandomStringUtils.randomAlphabetic(10);
        device2.setName(deviceName3);
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2);
        deviceUpdate2.setNetworkId(created.getId());

        deviceService.deviceSave(deviceUpdate);
        deviceService.deviceSave(deviceUpdate1);
        deviceService.deviceSave(deviceUpdate2);
        handleListDeviceRequest();
        ListDeviceRequest listDeviceRequest = new ListDeviceRequest();
        listDeviceRequest.setName(deviceName1);
        deviceService.list(listDeviceRequest)
                .thenAccept(devices -> {
                    assertNotNull(devices);
                    assertEquals(devices.size(), 1);
                    assertEquals(device.getDeviceId(), devices.get(0).getDeviceId());
                    assertEquals(device.getName(), devices.get(0).getName());
                }).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_save_and_find_by_network_id() throws Exception {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);

        UserVO user1 = new UserVO();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1 = userService.createUser(user1, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetworkId(network.getId());

        final NetworkVO network1 = new NetworkVO();
        network1.setName("" + randomUUID());
        NetworkVO created1 = networkService.create(network1);
        assertThat(created1.getId(), notNullValue());
        userService.assignNetwork(user1.getId(), network1.getId());
        deviceUpdate1.setNetworkId(network1.getId());

        deviceService.deviceSave(deviceUpdate);
        deviceService.deviceSave(deviceUpdate1);
        handleListDeviceRequest();
        deviceService.list(new ListDeviceRequest(network1.getId()))
                .thenAccept(devices -> {
                    assertNotNull(devices);
                    assertNotEquals(0, devices.size());
                    assertEquals(device1.getDeviceId(), devices.get(0).getDeviceId());
                    assertNotNull(devices.get(0).getNetworkId());
                    assertEquals(network1.getId(), devices.get(0).getNetworkId());
                }).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void should_update_device_data() throws Exception {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());
        deviceUpdate.setData(new JsonStringWrapper("{'data': 'data'}"));

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetworkId(network.getId());

        deviceService.deviceSave(deviceUpdate);

        final HivePrincipal principal = new HivePrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));
        deviceUpdate.setData(null);
        
        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
        
        DeviceVO deviceVO = deviceService.findById(device.getDeviceId());
                
        assertNull(deviceVO.getData());
    }


    @Test
    public void should_delete_device() {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);

        deviceUpdate.setNetworkId(created.getId());
        deviceService.deviceSave(deviceUpdate);
        DeviceVO existingDevice = deviceService.findByIdWithPermissionsCheck(device.getDeviceId(), null);
        assertNotNull(existingDevice);

        deviceService.deleteDevice(device.getDeviceId());
        existingDevice = deviceService.findByIdWithPermissionsCheck(device.getDeviceId(), null);
        assertNull(existingDevice);
    }

    @Test
    public void should_return_device_count() {
        final DeviceVO device0 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate0 = DeviceFixture.createDevice(device0.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        final DeviceVO device2 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate2 = DeviceFixture.createDevice(device2.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate0.setNetworkId(network.getId());
        deviceUpdate1.setNetworkId(network.getId());
        deviceUpdate2.setNetworkId(network.getId());

        deviceService.deviceSave(deviceUpdate0);
        deviceService.deviceSave(deviceUpdate1);
        deviceService.deviceSave(deviceUpdate2);

        long count = deviceService.getAllowedDevicesCount(null, Arrays.asList(device0.getDeviceId(), device1.getDeviceId(), device2.getDeviceId()));
        assertEquals(3, count);
    }


    /**
     * Test checks that unauthorized user can't modify device
     */
    @Test
    public void should_throw_HiveException_when_user_is_unauthorized() throws Exception {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.UNAUTHORIZED_REASON_PHRASE);

        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());
        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        deviceUpdate.setNetworkId(created.getId());
        deviceService.deviceSave(deviceUpdate);

        final HivePrincipal principal = new HivePrincipal();

        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
    }

    private void handleListDeviceRequest() {
        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            ListDeviceRequest req = request.getBody().cast(ListDeviceRequest.class);
            final List<DeviceVO> devices =
                    deviceDao.list(req.getName(), req.getNamePattern(),
                            req.getNetworkId(), req.getNetworkName(),
                            req.getSortField(), req.getSortOrderAsc(),
                            req.getTake(), req.getSkip(), req.getPrincipal());

            return Response.newBuilder()
                    .withBody(new ListDeviceResponse(devices))
                    .buildSuccess();
        });
    }

}
