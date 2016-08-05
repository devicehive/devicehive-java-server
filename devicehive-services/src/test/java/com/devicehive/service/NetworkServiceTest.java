package com.devicehive.service;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.vo.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NetworkServiceTest extends AbstractResourceTest {

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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String namePrefix;

    @Before
    public void setUp() throws Exception {
        namePrefix = RandomStringUtils.randomAlphabetic(10);
    }

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
        network.setName(Optional.of("network"));

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
        update.setKey(Optional.of("key"));
        update.setName(Optional.of("name"));
        update.setDescription(Optional.of("description"));

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
    public void should_return_list_of_networks() throws Exception {
        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", null, true, 10, 0, null);
        assertThat(networks, hasSize(10));
    }

    @Test
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
        List<NetworkVO> networks = networkService.list(randomNetwork.getRight(), null, null, true, 10, 0, null);
        assertThat(networks, hasSize(1));
        assertThat(networks.get(0).getId(), equalTo(randomNetwork.getKey()));
        assertThat(networks.get(0).getName(), equalTo(randomNetwork.getRight()));
    }

    @Test
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

        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, null);
        assertThat(networks, hasSize(count));
        assertThat(networks,
                hasItems(new CustomTypeSafeMatcher<NetworkVO>(String.format("expected '%s' word in name", namePrefix)) {
                    @Override
                    protected boolean matchesSafely(NetworkVO item) {
                        return item.getName().contains(namePrefix);
                    }
                }));
    }

    @Test
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
        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", "description", true, 100, 0, null);
        assertThat(networks, hasSize(descriptions.size()));

        assertThat(networks.get(0).getDescription(), equalTo("a"));
        assertThat(networks.get(1).getDescription(), equalTo("b"));
        assertThat(networks.get(2).getDescription(), equalTo("c"));
        assertThat(networks.get(3).getDescription(), equalTo("d"));
        assertThat(networks.get(4).getDescription(), equalTo("e"));

        networks = networkService.list(null, namePrefix + "%", "description", false, 100, 0, null);
        assertThat(networks, hasSize(descriptions.size()));

        assertThat(networks.get(0).getDescription(), equalTo("e"));
        assertThat(networks.get(1).getDescription(), equalTo("d"));
        assertThat(networks.get(2).getDescription(), equalTo("c"));
        assertThat(networks.get(3).getDescription(), equalTo("b"));
        assertThat(networks.get(4).getDescription(), equalTo("a"));

    }

    @Test
    public void should_correctly_apply_skip_limit_params() throws Exception {
        for (int i = 0; i < 100; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            network.setEntityVersion((long) i);
            NetworkVO created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        List<NetworkVO> all = networkService.list(null, namePrefix + "%", "entityVersion", true, 100, 0, null);
        assertThat(all, hasSize(100));

        List<NetworkVO> sliced = networkService.list(null, namePrefix + "%", "entityVersion", true, 20, 30, null);
        assertThat(sliced, hasSize(20));
        List<NetworkVO> expected = all.stream().skip(30).limit(20).collect(Collectors.toList());
        assertThat(sliced, contains(expected.toArray(new NetworkVO[expected.size()])));
    }

    @Test
    public void should_not_return_list_of_networks_for_device() throws Exception {
        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NO_ACCESS_TO_NETWORK);

        HivePrincipal principal = new HivePrincipal(new DeviceVO());
        networkService.list(null, null, null, true, 100, 0, principal);
    }

    @Test
    public void should_return_networks_only_for_user() throws Exception {
        UserVO user1 = new UserVO();
        user1.setLogin("user1" + RandomStringUtils.randomAlphabetic(10));
        user1 = userService.createUser(user1, "123");
        Set<String> expectedNames = new HashSet<>();
        for (int i = 0; i < 10; i++) {
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
        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(namePrefix + randomUUID());
            NetworkVO created = networkService.create(network);
            userService.assignNetwork(user2.getId(), created.getId());
        }

        List<NetworkVO> all = networkService.list(null, namePrefix + "%", null, true, 100, 0, null);
        assertThat(all, hasSize(20));

        HivePrincipal principal = new HivePrincipal(user1);
        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, principal);
        assertThat(networks, hasSize(10));

        Set<String> names = networks.stream().map(NetworkVO::getName).collect(Collectors.toSet());
        assertThat(names, equalTo(expectedNames));
    }

    @Test
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

        HivePrincipal principal = new HivePrincipal(user1);
        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, principal);
        assertThat(networks, hasSize(20));
    }

    @Test
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

        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setNetworkIdsCollection(allowedIds);
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setPermissions(Collections.singleton(permission));

        HivePrincipal principal = new HivePrincipal(accessKey);
        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, principal);
        assertThat(networks, hasSize(allowedIds.size()));
        Set<Long> ids = networks.stream().map(NetworkVO::getId).collect(Collectors.toSet());
        assertThat(allowedIds, equalTo(ids));
    }

    @Test
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

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        HivePrincipal principal = new HivePrincipal(accessKey);

        List<NetworkVO> networks = networkService.list(null, namePrefix + "%", null, true, 200, 0, principal);
        assertThat(networks, hasSize(assignedToUserCount));
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
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
    public void should_not_return_network_with_devices_and_device_classes_for_unassigned_user_access_key() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, nullValue());
    }

    @Test
    public void should_return_network_without_devices_if_access_key_does_not_have_permissions() throws Exception {
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray("do nothing");
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        NetworkWithUsersAndDevicesVO returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), is(empty()));
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

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setNetworkIdsCollection(Arrays.asList(-1L, -2L));
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(created));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        DeviceUpdate device = new DeviceUpdate();
        device.setName(Optional.ofNullable("allowed_device"));
        device.setGuid(Optional.ofNullable(randomUUID().toString()));
        device.setDeviceClass(Optional.ofNullable(dc));
        device.setNetwork(Optional.ofNullable(created));
        DeviceNotification notification = deviceService.deviceSave(device, Collections.emptySet());

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setNetworkIdsCollection(Collections.singleton(created.getId()));
        permission.setDeviceGuidsCollection(Collections.singleton(notification.getDeviceGuid()));
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
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
        dc.setName(Optional.ofNullable(randomUUID().toString()));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(Optional.ofNullable(randomUUID().toString()));
            device.setGuid(Optional.ofNullable(randomUUID().toString()));
            device.setDeviceClass(Optional.ofNullable(dc));
            device.setNetwork(Optional.ofNullable(created));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setDeviceGuidsCollection(Collections.singleton("-1"));
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
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

        AccessKeyVO accessKey = new AccessKeyVO();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setNetworkIdsCollection(Arrays.asList(first.getId(), -1L, -2L));
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, false);

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NETWORK_CREATION_NOT_ALLOWED);

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        networkService.createOrVerifyNetwork(Optional.ofNullable(network));
    }

    @Test
    public void should_create_new_network_when_creates_or_verifies_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        NetworkVO created = networkService.createOrVerifyNetwork(Optional.ofNullable(network));
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());
        assertThat(created.getName(), equalTo(network.getName()));
    }

    @Test
    public void should_verify_network_key_by_id_when_creates_or_verifies_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);
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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);
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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);
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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, false);

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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, false);

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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);

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
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);

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

    @Test
    public void should_verify_network_key_if_access_key_has_access_to_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);

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

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setUser(user);
        accessKey.setPermissions(Collections.singleton(new AccessKeyPermissionVO()));

        NetworkVO stored = networkService.createOrVerifyNetworkByKey(Optional.ofNullable(created), accessKey);
        assertThat(created.getId(), equalTo(stored.getId()));
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_access_key_does_not_have_access_to_network_when_verifies() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        NetworkVO created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setUser(user);
        accessKey.setPermissions(Collections.singleton(new AccessKeyPermissionVO()));

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.NO_ACCESS_TO_NETWORK);

        networkService.createOrVerifyNetworkByKey(Optional.ofNullable(created), accessKey);
    }
}
