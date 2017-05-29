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
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.rpc.ListNetworkRequest;
import com.devicehive.model.rpc.ListNetworkResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Constants.ALLOW_NETWORK_AUTO_CREATE;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class NetworkServiceTest extends AbstractResourceTest {

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Autowired
    private NetworkService networkService;
    @Autowired
    private UserService userService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private NetworkDao networkDao;

    @Mock
    private RequestHandler requestHandler;

    private ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);
        namePrefix = RandomStringUtils.randomAlphabetic(10);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String namePrefix;

    @Test
    public void should_throw_IllegalParametersException_when_create_network_with_id_provided() throws Exception {
        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.ID_NOT_ALLOWED);

        NetworkVO network = new NetworkVO();
        network.setId(1L);
        networkService.create(network);
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_network_with_name_already_exists() throws Exception {
        String name = "myNetwork" + RandomStringUtils.randomAlphabetic(10);

        NetworkVO network = new NetworkVO();
        network.setName(name);
        networkService.create(network);

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.DUPLICATE_NETWORK);

        network = new NetworkVO();
        network.setName(name);
        networkService.create(network);
    }

    @Test
    public void should_create_network() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());

        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        assertThat(created.getName(), is(network.getName()));
        assertThat(created.getDescription(), is(network.getDescription()));

        created = networkDao.find(created.getId());
        assertThat(created.getName(), is(network.getName()));
        assertThat(created.getDescription(), is(network.getDescription()));
    }

    @Test
    public void should_delete_network() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());

        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        boolean deleted = networkService.delete(created.getId());
        assertTrue(deleted);

        created = networkDao.find(created.getId());
        assertThat(created, nullValue());
    }

    @Test
    public void should_throw_NoSuchElementException_when_update_non_existent_network() throws Exception {
        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(String.format(Messages.NETWORK_NOT_FOUND, -1));

        NetworkUpdate network = new NetworkUpdate();
        network.setName("network");

        networkService.update(-1L, network);
    }

    @Test
    public void should_update_network() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        NetworkUpdate update = new NetworkUpdate();
        update.setKey("key");
        update.setName("name");
        update.setDescription("description");

        NetworkVO updated = networkService.update(created.getId(), update);
        assertThat(created.getId(), is(updated.getId()));
        assertThat(update.getName().get(), is(updated.getName()));
        assertThat(update.getDescription().get(), is(updated.getDescription()));
        assertThat(update.getKey().get(), is(updated.getKey()));

        network = networkDao.find(updated.getId());
        assertThat(update.getName().get(), is(network.getName()));
        assertThat(update.getDescription().get(), is(network.getDescription()));
        assertThat(update.getKey().get(), is(network.getKey()));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_list_of_networks() throws Exception {
        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        handleListNetworkRequest();
        networkService.list(null, namePrefix + "%", null, true, 10, 0, null)
                .thenAccept(networks -> assertThat(networks, hasSize(10))).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_filter_networks_by_name() throws Exception {
        List<Pair<Long, String>> names = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
            names.add(Pair.of(created.getId(), created.getName()));
        }
        int index = new Random().nextInt(10);
        Pair<Long, String> randomNetwork = names.get(index);
        handleListNetworkRequest();
        networkService.list(randomNetwork.getRight(), null, null, true, 10, 0, null)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(1));
                    assertThat(networks.get(0).getId(), equalTo(randomNetwork.getKey()));
                    assertThat(networks.get(0).getName(), equalTo(randomNetwork.getRight()));
                }).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_filter_networks_by_name_pattern() throws Exception {
        for (int i = 0; i < 20; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(RandomStringUtils.randomAlphabetic(20));
            network.setDescription("network description_" + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        int count = new Random().nextInt(30) + 1;
        for (int i = 0; i < count; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + RandomStringUtils.randomAlphabetic(10));
            network.setDescription("network description_" + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        handleListNetworkRequest();
        networkService.list(null, namePrefix + "%", null, true, 100, 0, null)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(count));
                    assertThat(networks,
                            hasItems(new CustomTypeSafeMatcher<NetworkVO>(String.format("expected '%s' word in name", namePrefix)) {
                                @Override
                                protected boolean matchesSafely(NetworkVO item) {
                                    return item.getName().contains(namePrefix);
                                }
                            }));
                }).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_sort_networks() throws Exception {
        List<String> descriptions = asList("a", "b", "c", "d", "e");
        Collections.shuffle(descriptions);
        descriptions.forEach(descr -> {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setDescription(descr);
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        });
        handleListNetworkRequest();
        networkService.list(null, namePrefix + "%", "description", true, 100, 0, null)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(descriptions.size()));

                    assertThat(networks.get(0).getDescription(), equalTo("a"));
                    assertThat(networks.get(1).getDescription(), equalTo("b"));
                    assertThat(networks.get(2).getDescription(), equalTo("c"));
                    assertThat(networks.get(3).getDescription(), equalTo("d"));
                    assertThat(networks.get(4).getDescription(), equalTo("e"));
                }).get(5, TimeUnit.SECONDS);

        networkService.list(null, namePrefix + "%", "description", false, 100, 0, null)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(descriptions.size()));

                    assertThat(networks.get(0).getDescription(), equalTo("e"));
                    assertThat(networks.get(1).getDescription(), equalTo("d"));
                    assertThat(networks.get(2).getDescription(), equalTo("c"));
                    assertThat(networks.get(3).getDescription(), equalTo("b"));
                    assertThat(networks.get(4).getDescription(), equalTo("a"));
                }).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(2)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_correctly_apply_skip_limit_params() throws Exception {
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setEntityVersion((long) i);
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        handleListNetworkRequest();
        networkService.list(null, namePrefix + "%", "entityVersion", true, 100, 0, null)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(100));

                    try {
                        networkService.list(null, namePrefix + "%", "entityVersion", true, 20, 30, null)
                                .thenAccept(sliced -> {
                                    assertThat(sliced, hasSize(20));
                                    List<NetworkVO> expected = networks.stream().skip(30).limit(20).collect(Collectors.toList());
                                    assertThat(sliced, contains(expected.toArray(new NetworkVO[expected.size()])));
                                }).get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(2)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_networks_only_for_user() throws Exception {
        UserVO user1 = new UserVO();
        user1.setLogin("user1" + RandomStringUtils.randomAlphabetic(10));
        user1 = userService.createUser(user1, "123");
        Set<String> expectedNames = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            String name = namePrefix + randomUUID();
            NetworkVO network = new NetworkVO();
            network.setName(name);
            expectedNames.add(name);
            NetworkVO created = networkService.create(network);
            userService.assignNetwork(user1.getId(), created.getId());
        }

        UserVO user2 = new UserVO();
        user2.setLogin("user2" + RandomStringUtils.randomAlphabetic(10));
        user2 = userService.createUser(user2, "123");
        for (int i = 0; i < 5; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            NetworkVO created = networkService.create(network);
            userService.assignNetwork(user2.getId(), created.getId());
        }
        handleListNetworkRequest();
        networkService.list(null, namePrefix + "%", null, true, 100, 0, null)
                .thenAccept(networks -> assertThat(networks, hasSize(10))).get(5, TimeUnit.SECONDS);

        HivePrincipal principal = new HivePrincipal(user1);
        networkService.list(null, namePrefix + "%", null, true, 100, 0, principal)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(5));
                    Set<String> names = networks.stream().map(NetworkVO::getName).collect(Collectors.toSet());
                    assertThat(names, equalTo(expectedNames));
                }).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(2)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_all_networks_for_admin() throws Exception {
        UserVO user1 = new UserVO();
        user1.setLogin("user1" + RandomStringUtils.randomAlphabetic(10));
        user1.setRole(UserRole.ADMIN);
        user1 = userService.createUser(user1, "123");
        for (int i = 0; i < 10; i++) {
            String name = namePrefix + randomUUID();
            NetworkVO network = new NetworkVO();
            network.setName(name);
            NetworkVO created = networkService.create(network);
            userService.assignNetwork(user1.getId(), created.getId());
        }

        UserVO user2 = new UserVO();
        user2.setLogin("user2" + RandomStringUtils.randomAlphabetic(10));
        user2 = userService.createUser(user2, "123");
        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            NetworkVO created = networkService.create(network);
            userService.assignNetwork(user2.getId(), created.getId());
        }
        handleListNetworkRequest();
        HivePrincipal principal = new HivePrincipal(user1);
        networkService.list(null, namePrefix + "%", null, true, 100, 0, principal)
                .thenAccept(networks -> assertThat(networks, hasSize(20))).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_only_allowed_networks_for_access_key() throws Exception {
        Set<Long> allowedIds = new HashSet<>();

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        allowedIds.add(created.getId());

        for (int i = 0; i < 100; i++) {
            network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
            if (Math.random() < 0.5) {
                allowedIds.add(created.getId());
            }
        }

        handleListNetworkRequest();
        HivePrincipal principal = new HivePrincipal();
        principal.setNetworkIds(allowedIds);
        networkService.list(null, namePrefix + "%", null, true, 100, 0, principal)
                .thenAccept(networks -> {
                    assertThat(networks, hasSize(allowedIds.size()));
                    Set<Long> ids = networks.stream().map(NetworkVO::getId).collect(Collectors.toSet());
                    assertThat(allowedIds, equalTo(ids));
                }).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_networks_for_access_key_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin("user1" + RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }

        int assignedToUserCount = 20;
        for (int i = 0; i < assignedToUserCount; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
            userService.assignNetwork(user.getId(), created.getId());
        }

//        AccessKeyVO accessKey = new AccessKeyVO();
//        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
//        accessKey.setPermissions(Collections.singleton(permission));
//        accessKey.setUser(user);

        HivePrincipal principal = new HivePrincipal(user);
        handleListNetworkRequest();
        networkService.list(null, namePrefix + "%", null, true, 200, 0, principal)
                .thenAccept(networks -> assertThat(networks, hasSize(assignedToUserCount))).get(5, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void should_return_network_with_devices_and_device_classes_for_admin() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(network.getId());
            deviceService.deviceSave(device);
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().orElse(null)));
        });
    }

    @Test
    public void should_return_null_network_if_user_is_not_an_admin() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);

        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(network.getId());
            deviceService.deviceSave(device);
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, nullValue());
    }

    @Test
    public void should_return_network_for_if_client_is_assigned_to_it() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), created.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(network.getId());
            deviceService.deviceSave(device);
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().orElse(null)));
        });
    }

    @Test
    public void should_return_network_with_devices_and_device_classes_for_admin_access_key() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(network.getId());
            deviceService.deviceSave(device);
        }


        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().orElse(null)));
        });
    }

    @Test
    public void should_return_network_with_devices_and_device_classes_for_assigned_user_access_key() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(network.getId());
            deviceService.deviceSave(device);
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        for (DeviceVO device : returnedNetwork.getDevices()) {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getId(), notNullValue());
            assertThat(device.getDeviceClass().getName(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().orElse(null)));
        }
    }

    @Test
    public void should_not_return_network_with_devices_if_access_key_does_not_have_permissions() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        userService.assignNetwork(user.getId(), created.getId());

        HivePrincipal principal = new HivePrincipal(user);
        principal.setNetworkIds(new HashSet<>(Arrays.asList(-1L, -2L)));
        HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, nullValue());
    }

    @Test
    public void should_return_network_only_with_permitted_devices_for_access_key() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        userService.assignNetwork(user.getId(), created.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(created.getId());
            deviceService.deviceSave(device);
        }

        DeviceUpdate device = new DeviceUpdate();
        device.setName("allowed_device");
        device.setGuid(randomUUID().toString());
        device.setDeviceClass(dc);
        device.setNetworkId(created.getId());
        DeviceNotification notification = deviceService.deviceSave(device);

        HivePrincipal principal = new HivePrincipal(user);
        principal.setNetworkIds(new HashSet<>(Collections.singleton(created.getId())));
        principal.setDeviceGuids(new HashSet<>(Collections.singleton(notification.getDeviceGuid())));
        HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(1));
        assertThat(returnedNetwork.getDevices(), hasItem(new CustomTypeSafeMatcher<DeviceVO>("expect device") {
            @Override
            protected boolean matchesSafely(DeviceVO item) {
                return item.getGuid().equals(notification.getDeviceGuid());
            }
        }));
    }

    @Test
    public void should_return_network_without_devices_if_access_key_does_not_have_device_guid_in_permissions() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        userService.assignNetwork(user.getId(), created.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(randomUUID().toString());
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(randomUUID().toString());
            device.setGuid(randomUUID().toString());
            device.setDeviceClass(dc);
            device.setNetworkId(created.getId());
            deviceService.deviceSave(device);
        }

        HivePrincipal principal = new HivePrincipal(user);
        principal.setDeviceGuids(new HashSet<>(Collections.singleton("-1")));
        HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), is(empty()));
    }

    @Test
    public void should_return_permitted_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO first = networkService.create(network);
        assertThat(first.getId(), notNullValue());
        userService.assignNetwork(user.getId(), first.getId());

        network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO second = networkService.create(network);
        assertThat(second.getId(), notNullValue());
        userService.assignNetwork(user.getId(), second.getId());

        HivePrincipal principal = new HivePrincipal(user);
        principal.setNetworkIds(new HashSet<>(Arrays.asList(first.getId(), -1L, -2L)));
        HiveAuthentication authentication = new HiveAuthentication(principal);
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(first.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getId(), equalTo(first.getId()));

        returnedNetwork = networkService.getWithDevicesAndDeviceClasses(second.getId(), authentication);
        assertThat(returnedNetwork, nullValue());
    }

    @Test
    public void should_do_nothing_when_creates_or_verifies_network_if_network_is_null() throws Exception {
        assertThat(networkService.createOrVerifyNetwork(Optional.ofNullable(null)), nullValue());
    }

    @Test
    public void should_throw_IllegalParametersException_if_id_provided_when_creates_or_verifies_network() throws Exception {
        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.INVALID_REQUEST_PARAMETERS);

        NetworkVO network = new NetworkVO();
        network.setId(-1L);
        networkService.createOrVerifyNetwork(Optional.ofNullable(network));
    }

    @Ignore("JavaScript integration test '#Create Auto Create (incl. Legacy Equipment) should auto-create network and device class' fails with such behavior")
    @Test
    public void should_throw_ActionNotAllowedException_if_network_auto_creation_is_not_allowed_when_creates_or_verifies_network() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, false);

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NETWORK_CREATION_NOT_ALLOWED);

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        networkService.createOrVerifyNetwork(Optional.ofNullable(network));
    }

    @Test
    public void should_create_new_network_when_creates_or_verifies_network() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, true);

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.createOrVerifyNetwork(Optional.ofNullable(network));
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());
        assertThat(created.getName(), equalTo(network.getName()));
    }

    @Test
    public void should_verify_network_key_by_id_when_creates_or_verifies_network() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, true);
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        NetworkVO created = networkService.createOrVerifyNetwork(Optional.ofNullable(network));
        assertThat(created, notNullValue());

        NetworkVO verified = networkService.createOrVerifyNetwork(Optional.ofNullable(created));
        assertThat(verified, notNullValue());
        assertThat(verified.getId(), equalTo(created.getId()));
        assertThat(verified.getName(), equalTo(created.getName()));
    }

    @Test
    public void should_verify_network_key_by_name_when_creates_or_verifies_network() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, true);
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        NetworkVO created = networkService.createOrVerifyNetwork(Optional.ofNullable(network));
        assertThat(created, notNullValue());

        Long networkId = created.getId();
        created.setId(null);

        NetworkVO verified = networkService.createOrVerifyNetwork(Optional.ofNullable(created));
        assertThat(verified, notNullValue());
        assertThat(verified.getId(), equalTo(networkId));
        assertThat(verified.getName(), equalTo(created.getName()));
    }

    @Test
    public void should_throw_ActionNotAllowedException_when_creates_or_verifies_network_if_network_key_is_corrupted() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, true);
        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        NetworkVO created = networkService.createOrVerifyNetwork(Optional.of(network));
        assertThat(created, notNullValue());

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.INVALID_NETWORK_KEY);

        created.setKey(randomUUID().toString());
        networkService.createOrVerifyNetwork(Optional.of(created));
    }

    @Test
    public void should_check_whether_user_is_admin_when_creates_or_updates_network_by_user() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, false);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());

        NetworkVO created = networkService.createOrUpdateNetworkByUser(Optional.ofNullable(network), user);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());
        assertThat(created.getName(), equalTo(network.getName()));
    }

    @Test
    public void should_throw_exception_when_updates_network_by_user_if_user_is_client() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, false);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NETWORK_CREATION_NOT_ALLOWED);

        networkService.createOrUpdateNetworkByUser(Optional.ofNullable(network), user);
    }

    @Test
    public void should_verify_network_if_client_has_access() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, true);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        userService.assignNetwork(user.getId(), created.getId());

        NetworkVO stored = networkService.createOrUpdateNetworkByUser(Optional.ofNullable(created), user);
        assertThat(created.getId(), equalTo(stored.getId()));
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_client_does_not_have_access_to_verifies_network() throws Exception {
        configurationService.save(ALLOW_NETWORK_AUTO_CREATE, true);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NO_ACCESS_TO_NETWORK);

        networkService.createOrUpdateNetworkByUser(Optional.ofNullable(created), user);
    }

    private void handleListNetworkRequest() {
        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            ListNetworkRequest req = request.getBody().cast(ListNetworkRequest.class);
            final List<NetworkVO> networks =
                    networkDao.list(req.getName(), req.getNamePattern(),
                            req.getSortField(), req.getSortOrderAsc(),
                            req.getTake(), req.getSkip(), req.getPrincipal());

            return Response.newBuilder()
                    .withBody(new ListNetworkResponse(networks))
                    .buildSuccess();
        });
    }
}
