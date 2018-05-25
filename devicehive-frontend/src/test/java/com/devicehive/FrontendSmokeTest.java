package com.devicehive;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDao;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.*;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.devicehive.model.enums.SortOrder.ASC;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FrontendSmokeTest extends AbstractResourceTest {

    private static final String DEFAULT_STATUS = "default_status";

    @Autowired
    private Environment env;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DeviceCommandService deviceCommandService;

    @Autowired
    private DeviceNotificationService notificationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private NetworkDao networkDao;

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

    @Test
    public void should_return_API_info() throws Exception {
        ApiInfoVO apiInfo = performRequest("info", "GET", emptyMap(), emptyMap(), null, OK, ApiInfoVO.class);
        assertThat(apiInfo.getServerTimestamp(), notNullValue());
        assertThat(apiInfo.getRestServerUrl(), nullValue());
        assertThat(apiInfo.getWebSocketServerUrl(), is(wsBaseUri() + "/websocket"));
    }

    @Test
    public void should_return_cluster_config() throws Exception {
        ClusterConfigVO clusterConfig = performRequest("info/config/cluster", "GET", emptyMap(), emptyMap(), null, OK, ClusterConfigVO.class);
        assertThat(clusterConfig, notNullValue());
        assertThat(clusterConfig.getBootstrapServers(), is(env.getProperty(Constants.BOOTSTRAP_SERVERS)));
        assertThat(clusterConfig.getZookeeperConnect(), is(env.getProperty(Constants.ZOOKEEPER_CONNECT)));
    }

    @Test
    public void should_save_configuration_property_and_return_by_name() throws Exception {
        String key = RandomStringUtils.randomAlphabetic(10);
        String val = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, val);

        String savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(val));
    }

    @Test
    public void should_update_config_property() throws Exception {
        String key = RandomStringUtils.randomAlphabetic(10);
        String val = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, val);

        String savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(val));

        String newVal = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, newVal);

        savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(newVal));
    }

    @Test
    public void should_delete_config_property() throws Exception {
        String key = RandomStringUtils.randomAlphabetic(10);
        String val = RandomStringUtils.randomAlphabetic(10);
        configurationService.save(key, val);

        String savedVal = configurationService.get(key);
        assertThat(savedVal, equalTo(val));

        configurationService.delete(key);

        savedVal = configurationService.get(key);
        assertThat(savedVal, Matchers.nullValue());
    }

    @Test
    public void should_find_commands_by_device_id() throws Exception {
        final List<String> deviceIds = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        final Date timestampSt = timestampService.getDate();
        final Date timestampEnd = timestampService.getDate();
        final String parameters = "{\"param1\":\"value1\",\"param2\":\"value2\"}";

        final Set<String> idsForSearch = new HashSet<>(Arrays.asList(
                deviceIds.get(0),
                deviceIds.get(2),
                deviceIds.get(3)));

        final Map<String, DeviceCommand> commandMap = idsForSearch.stream()
                .collect(Collectors.toMap(Function.identity(), deviceId -> {
                    DeviceCommand command = new DeviceCommand();
                    command.setId(System.nanoTime());
                    command.setDeviceId(deviceId);
                    command.setCommand(RandomStringUtils.randomAlphabetic(10));
                    command.setTimestamp(timestampService.getDate());
                    command.setParameters(new JsonStringWrapper(parameters));
                    command.setStatus(DEFAULT_STATUS);
                    return command;
                }));

        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            Set<String> foundDeviceIds = request.getBody().cast(CommandSearchRequest.class).getDeviceIds();
            CommandSearchResponse response = new CommandSearchResponse();
            response.setCommands(foundDeviceIds.stream().map(commandMap::get).collect(Collectors.toList()));
            return Response.newBuilder()
                    .withBody(response)
                    .buildSuccess();
        });

        deviceCommandService.find(idsForSearch, Collections.emptySet(), timestampSt, timestampEnd, DEFAULT_STATUS, null, null, null, null)
                .thenAccept(commands -> {
                    assertEquals(3, commands.size());
                    assertEquals(new HashSet<>(commandMap.values()), new HashSet<>(commands));
                })
                .get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void should_update_command() throws Exception {
        final DeviceCommand deviceCommand = new DeviceCommand();
        deviceCommand.setId(System.nanoTime());
        deviceCommand.setDeviceId(UUID.randomUUID().toString());
        deviceCommand.setCommand("command");
        deviceCommand.setParameters(new JsonStringWrapper("{'test':'test'}"));
        deviceCommand.setStatus(DEFAULT_STATUS);

        final DeviceCommandUpdate commandUpdate = new DeviceCommandUpdate();
        commandUpdate.setStatus("OK");

        when(requestHandler.handle(any(Request.class))).then(invocation -> Response.newBuilder()
                .buildSuccess());

        deviceCommandService.update(deviceCommand, commandUpdate).
                thenAccept(Assert::assertNull).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(2)).handle(argument.capture());
    }

    @Test
    public void should_insert_notification() throws Exception {
        final DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(System.nanoTime());
        deviceVO.setDeviceId(UUID.randomUUID().toString());

        final DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(System.nanoTime());
        deviceNotification.setTimestamp(new Date());
        deviceNotification.setNotification(RandomStringUtils.randomAlphabetic(10));
        deviceNotification.setDeviceId(deviceVO.getDeviceId());

        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationInsertResponse(deviceNotification))
                .buildSuccess());

        notificationService.insert(deviceNotification, deviceVO)
                .thenAccept(notification -> assertEquals(deviceNotification, notification))
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());

        NotificationInsertRequest request = argument.getValue().getBody().cast(NotificationInsertRequest.class);
        assertEquals(Action.NOTIFICATION_INSERT_REQUEST, request.getAction());
        assertEquals(deviceNotification, request.getDeviceNotification());
    }

    @Test
    public void should_find_notification() throws Exception {
        final String deviceId = UUID.randomUUID().toString();
        final long id = System.currentTimeMillis();
        final String notification = "Expected notification";
        final Date timestamp = new Date();
        final String parameters = "{\"param1\":\"value1\",\"param2\":\"value2\"}";

        final DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(id);
        deviceNotification.setDeviceId(deviceId);
        deviceNotification.setNotification(notification);
        deviceNotification.setTimestamp(timestamp);
        deviceNotification.setParameters(new JsonStringWrapper(parameters));

        // return response for any request
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationSearchResponse(Collections.singletonList(deviceNotification)))
                .buildSuccess());

        // call service method
        notificationService.findOne(id, deviceId)
                .thenAccept(opt -> {
                    assertTrue(opt.isPresent());
                    assertEquals(deviceId, opt.get().getDeviceId());
                    assertEquals(timestamp, opt.get().getTimestamp());
                    assertEquals(parameters, opt.get().getParameters().getJsonString());
                    assertEquals(notification, opt.get().getNotification());
                    assertEquals(Long.valueOf(id), opt.get().getId());
                })
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
        assertEquals(Action.NOTIFICATION_SEARCH_REQUEST, argument.getValue().getBody().getAction());

        NotificationSearchRequest request = argument.getValue().getBody().cast(NotificationSearchRequest.class);
        assertEquals(id, request.getId().longValue());
        assertEquals(deviceId, request.getDeviceId());
        assertNull(request.getStatus());
        assertNull(request.getNames());
        assertNull(request.getTimestampStart());
        assertNull(request.getTimestampEnd());
        assertNull(request.getTimestampEnd());
    }

    @Test
    public void should_save_and_find_devices_by_user() throws UnknownHostException {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user.setAllDeviceTypesAvailable(false);
        user = userService.createUser(user, VALID_PASSWORD);

        UserVO user1 = new UserVO();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1.setAllDeviceTypesAvailable(false);
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

        final DeviceTypeVO deviceType = new DeviceTypeVO();
        deviceType.setName("" + randomUUID());
        DeviceTypeVO createdType = deviceTypeService.create(deviceType);
        assertThat(createdType.getId(), notNullValue());
        userService.assignDeviceType(user.getId(), deviceType.getId());
        deviceUpdate.setDeviceTypeId(deviceType.getId());

        final DeviceTypeVO deviceType1 = new DeviceTypeVO();
        deviceType1.setName("" + randomUUID());
        DeviceTypeVO createdType1 = deviceTypeService.create(deviceType1);
        assertThat(createdType1.getId(), notNullValue());
        userService.assignDeviceType(user1.getId(), deviceType1.getId());
        deviceUpdate1.setDeviceTypeId(deviceType1.getId());

        final HivePrincipal principal = new HivePrincipal(user);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        deviceService.deviceSave(device.getDeviceId(), deviceUpdate);
        deviceService.deviceSave(device1.getDeviceId(), deviceUpdate1);

        final List<DeviceVO> devices = deviceService.findByIdWithPermissionsCheck(
                Arrays.asList(device.getDeviceId(), device1.getDeviceId()), principal);
        assertNotNull(devices);
        assertEquals(1, devices.size());
        assertEquals(devices.get(0).getDeviceId(), device.getDeviceId());
    }

    @Test
    public void should_save_and_find_by_device_id() throws UnknownHostException {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());

        final DeviceVO device1 = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate1 = DeviceFixture.createDevice(device1.getDeviceId());

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user.setAllDeviceTypesAvailable(false);
        user = userService.createUser(user, VALID_PASSWORD);

        UserVO user1 = new UserVO();
        user1.setLogin(RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.CLIENT);
        user1.setAllDeviceTypesAvailable(false);
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

        final DeviceTypeVO deviceType = new DeviceTypeVO();
        deviceType.setName("" + randomUUID());
        DeviceTypeVO createdType = deviceTypeService.create(deviceType);
        assertThat(createdType.getId(), notNullValue());
        userService.assignDeviceType(user.getId(), deviceType.getId());
        deviceUpdate.setDeviceTypeId(deviceType.getId());

        final DeviceTypeVO deviceType1 = new DeviceTypeVO();
        deviceType1.setName("" + randomUUID());
        DeviceTypeVO createdType1 = deviceTypeService.create(deviceType1);
        assertThat(createdType1.getId(), notNullValue());
        userService.assignDeviceType(user1.getId(), deviceType1.getId());
        deviceUpdate1.setDeviceTypeId(deviceType1.getId());

        deviceService.deviceSave(device.getDeviceId(), deviceUpdate);
        deviceService.deviceSave(device1.getDeviceId(), deviceUpdate1);

        HivePrincipal principal = new HivePrincipal();
        Set<Long> allowedDeviceTypes = new HashSet<>();
        allowedDeviceTypes.add(deviceType.getId());
        principal.setDeviceTypeIds(allowedDeviceTypes);
        final HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        final List<DeviceVO> devices = deviceService.findByIdWithPermissionsCheck(
                Arrays.asList(device.getDeviceId(), device1.getDeviceId()), principal);
        assertNotNull(devices);
        assertEquals(1, devices.size());
        assertEquals(devices.get(0).getDeviceId(), device.getDeviceId());
    }

    @Test
    public void should_update_device_data() throws Exception {
        final DeviceVO device = DeviceFixture.createDeviceVO();
        final DeviceUpdate deviceUpdate = DeviceFixture.createDevice(device.getDeviceId());
        deviceUpdate.setData(new JsonStringWrapper("{'data': 'data'}"));

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user.setAllDeviceTypesAvailable(false);
        user = userService.createUser(user, VALID_PASSWORD);

        final NetworkVO network = new NetworkVO();
        network.setName("" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());
        deviceUpdate.setNetworkId(network.getId());

        final DeviceTypeVO deviceType = new DeviceTypeVO();
        deviceType.setName("" + randomUUID());
        DeviceTypeVO createdType = deviceTypeService.create(deviceType);
        assertThat(createdType.getId(), notNullValue());
        userService.assignDeviceType(user.getId(), deviceType.getId());
        deviceUpdate.setDeviceTypeId(deviceType.getId());

        deviceService.deviceSave(device.getDeviceId(), deviceUpdate);

        final HivePrincipal principal = new HivePrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));
        deviceUpdate.setData(null);

        deviceService.deviceSaveAndNotify(device.getDeviceId(), deviceUpdate, principal);

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

        final DeviceTypeVO deviceType = new DeviceTypeVO();
        deviceType.setName("" + randomUUID());
        DeviceTypeVO createdType = deviceTypeService.create(deviceType);
        deviceUpdate.setDeviceTypeId(createdType.getId());

        deviceService.deviceSave(device.getDeviceId(), deviceUpdate);
        DeviceVO existingDevice = deviceService.findByIdWithPermissionsCheck(device.getDeviceId(), null);
        assertNotNull(existingDevice);

        deviceService.deleteDevice(device.getDeviceId());
        existingDevice = deviceService.findByIdWithPermissionsCheck(device.getDeviceId(), null);
        assertNull(existingDevice);
    }

    @Test
    public void should_return_list_of_networks() throws Exception {
        String namePrefix = RandomStringUtils.randomAlphabetic(10);
        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            ListNetworkRequest req = request.getBody().cast(ListNetworkRequest.class);
            final List<NetworkVO> networks =
                    networkDao.list(req.getName(), req.getNamePattern(),
                            req.getSortField(), req.isSortOrderAsc(),
                            req.getTake(), req.getSkip(), req.getPrincipal());

            return Response.newBuilder()
                    .withBody(new ListNetworkResponse(networks))
                    .buildSuccess();
        });
        networkService.list(null, namePrefix + "%", null, ASC.name(), 10, 0, null)
                .thenAccept(networks -> assertThat(networks, hasSize(10))).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void should_delete_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, VALID_PASSWORD);

        String namePrefix = RandomStringUtils.randomAlphabetic(10);
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());

        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());

        final HivePrincipal principal = new HivePrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new HiveAuthentication(principal));

        boolean deleted = networkService.delete(created.getId(), true);
        assertTrue(deleted);

        created = networkDao.find(created.getId());
        assertThat(created, Matchers.nullValue());
    }

    @Test
    public void should_create_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));
        assertThat(user.getLoginAttempts(), equalTo(0));
    }

    @Test
    public void should_authenticate_user_successfully() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        UserVO authenticated = userService.authenticate(user.getLogin(), VALID_PASSWORD);
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getLoginAttempts(), equalTo(0));
        assertThat(authenticated.getLastLogin(), notNullValue());
    }

    @Test
    public void should_return_user_by_id() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        user = userService.findById(user.getId());
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));
    }

    @Test
    public void should_delete_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, VALID_PASSWORD);
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        assertTrue(userService.deleteUser(user.getId()));

        user = userService.findById(user.getId());
        assertThat(user, Matchers.nullValue());
    }

}
