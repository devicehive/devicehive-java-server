package com.devicehive.service;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.matcher.HiveExceptionMatcher;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.GenericDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.updates.NetworkUpdate;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Before;
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
    private GenericDAO genericDAO;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String namePrefix;

    @Before
    public void setUp() throws Exception {
        namePrefix = RandomStringUtils.random(10);
    }

    @Test
    public void should_throw_HiveException_when_create_network_id() throws Exception {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.ID_NOT_ALLOWED);
        expectedException.expect(HiveExceptionMatcher.code(400));

        Network network = new Network();
        network.setId(1L);
        networkService.create(network);
    }

    @Test
    public void should_throw_HiveException_if_network_with_name_already_exists() throws Exception {
        Network network = new Network();
        network.setName("myNetwork");
        networkService.create(network);

        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.DUPLICATE_NETWORK);
        expectedException.expect(HiveExceptionMatcher.code(403));

        network = new Network();
        network.setName("myNetwork");
        networkService.create(network);
    }

    @Test
    public void should_create_network() throws Exception {
        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());

        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        assertThat(created.getName(), is(network.getName()));
        assertThat(created.getDescription(), is(network.getDescription()));

        created = genericDAO.find(Network.class, created.getId());
        assertThat(created.getName(), is(network.getName()));
        assertThat(created.getDescription(), is(network.getDescription()));
    }

    @Test
    public void should_delete_network() throws Exception {
        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());

        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        boolean deleted = networkService.delete(created.getId());
        assertTrue(deleted);

        created = genericDAO.find(Network.class, created.getId());
        assertThat(created, nullValue());
    }

    @Test
    public void should_throw_HiveException_when_update_non_existent_network() throws Exception {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(String.format(Messages.NETWORK_NOT_FOUND, -1));
        expectedException.expect(HiveExceptionMatcher.code(404));

        NetworkUpdate network = new NetworkUpdate();
        network.setName(new NullableWrapper<>("network"));

        networkService.update(-1L, network);
    }

    @Test
    public void should_update_network() throws Exception {
        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        network.setDescription("network description_" + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        NetworkUpdate update = new NetworkUpdate();
        update.setKey(new NullableWrapper<>("key"));
        update.setName(new NullableWrapper<>("name"));
        update.setDescription(new NullableWrapper<>("description"));

        Network updated = networkService.update(created.getId(), update);
        assertThat(created.getId(), is(updated.getId()));
        assertThat(update.getName().getValue(), is(updated.getName()));
        assertThat(update.getDescription().getValue(), is(updated.getDescription()));
        assertThat(update.getKey().getValue(), is(updated.getKey()));

        network = genericDAO.find(Network.class, updated.getId());
        assertThat(update.getName().getValue(), is(network.getName()));
        assertThat(update.getDescription().getValue(), is(network.getDescription()));
        assertThat(update.getKey().getValue(), is(network.getKey()));
    }

    @Test
    public void should_return_list_of_networks() throws Exception {
        for (int i = 0; i < 10; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        List<Network> networks = networkService.list(null, null, null, true, 10, 0, null);
        assertThat(networks, hasSize(10));
    }

    @Test
    public void should_filter_networks_by_name() throws Exception {
        List<Pair<Long, String>> names = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
            names.add(Pair.of(created.getId(), created.getName()));
        }
        int index = new Random().nextInt(10);
        Pair<Long, String> randomNetwork = names.get(index);
        List<Network> networks = networkService.list(randomNetwork.getRight(), null, null, true, 10, 0, null);
        assertThat(networks, hasSize(1));
        assertThat(networks.get(0).getId(), equalTo(randomNetwork.getKey()));
        assertThat(networks.get(0).getName(), equalTo(randomNetwork.getRight()));
    }

    @Test
    public void should_filter_networks_by_name_pattern() throws Exception {
        for (int i = 0; i < 20; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            network.setDescription("network description_" + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        int count = new Random().nextInt(30) + 1;
        for (int i = 0; i < count; i++) {
            Network network = new Network();
            network.setName("some special network " + randomUUID());
            network.setDescription("network description_" + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }

        List<Network> networks = networkService.list(null, "%special%", null, true, 100, 0, null);
        assertThat(networks, hasSize(count));
        assertThat(networks, hasItems(new CustomTypeSafeMatcher<Network>("expected 'special' word in name") {
            @Override
            protected boolean matchesSafely(Network item) {
                return item.getName().contains("some special network");
            }
        }));
    }

    @Test
    public void should_sort_networks() throws Exception {
        List<String> descriptions = asList("a", "b", "c", "d", "e");
        Collections.shuffle(descriptions);
        descriptions.forEach(descr -> {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            network.setDescription(descr);
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        });
        List<Network> networks = networkService.list(null, namePrefix + "%", "description", true, 100, 0, null);
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
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }
        List<Network> all = networkService.list(null, namePrefix + "%", null, true, 100, 0, null);
        assertThat(all, hasSize(100));

        List<Network> sliced = networkService.list(null, namePrefix + "%", null, true, 20, 30, null);
        assertThat(sliced, hasSize(20));
        List<Network> expected = all.stream().skip(30).limit(20).collect(Collectors.toList());
        assertThat(sliced, contains(expected.toArray(new Network[expected.size()])));
    }

    @Test
    public void should_not_return_list_of_networks_for_device() throws Exception {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage("Can not get access to networks");
        expectedException.expect(HiveExceptionMatcher.code(403));

        HivePrincipal principal = new HivePrincipal(new Device());
        networkService.list(null, null, null, true, 100, 0, principal);
    }

    @Test
    public void should_return_networks_only_for_user() throws Exception {
        User user1 = new User();
        user1.setLogin("user1");
        user1 = userService.createUser(user1, "123");
        List<String> expectedNames = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String name = namePrefix + randomUUID();
            Network network = new Network();
            network.setName(name);
            expectedNames.add(name);
            Network created = networkService.create(network);
            userService.assignNetwork(user1.getId(), created.getId());
        }

        User user2 = new User();
        user2.setLogin("user2");
        user2 = userService.createUser(user2, "123");
        for (int i = 0; i < 10; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            Network created = networkService.create(network);
            userService.assignNetwork(user2.getId(), created.getId());
        }

        List<Network> all = networkService.list(null, namePrefix + "%", null, true, 100, 0, null);
        assertThat(all, hasSize(20));

        HivePrincipal principal = new HivePrincipal(user1);
        List<Network> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, principal);
        assertThat(networks, hasSize(10));

        List<String> names = networks.stream().map(Network::getName).collect(Collectors.toList());
        assertThat(names, equalTo(expectedNames));
    }

    @Test
    public void should_return_all_networks_for_admin() throws Exception {
        User user1 = new User();
        user1.setLogin("user1");
        user1.setRole(UserRole.ADMIN);
        user1 = userService.createUser(user1, "123");
        for (int i = 0; i < 10; i++) {
            String name = namePrefix + randomUUID();
            Network network = new Network();
            network.setName(name);
            Network created = networkService.create(network);
            userService.assignNetwork(user1.getId(), created.getId());
        }

        User user2 = new User();
        user2.setLogin("user2");
        user2 = userService.createUser(user2, "123");
        for (int i = 0; i < 10; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            Network created = networkService.create(network);
            userService.assignNetwork(user2.getId(), created.getId());
        }

        HivePrincipal principal = new HivePrincipal(user1);
        List<Network> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, principal);
        assertThat(networks, hasSize(20));
    }

    @Test
    public void should_return_only_allowed_networks_for_access_key() throws Exception {
        Set<Long> allowedIds = new HashSet<>();

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        allowedIds.add(created.getId());

        for (int i = 0; i < 100; i++) {
            network = new Network();
            network.setName(namePrefix + randomUUID());
            created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
            if (Math.random() < 0.5) {
                allowedIds.add(created.getId());
            }
        }

        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setNetworkIds(allowedIds);
        AccessKey accessKey = new AccessKey();
        accessKey.setPermissions(Collections.singleton(permission));

        HivePrincipal principal = new HivePrincipal(accessKey);
        List<Network> networks = networkService.list(null, namePrefix + "%", null, true, 100, 0, principal);
        assertThat(networks, hasSize(allowedIds.size()));
        Set<Long> ids = networks.stream().map(Network::getId).collect(Collectors.toSet());
        assertThat(allowedIds, equalTo(ids));
    }

    @Test
    public void should_return_networks_for_access_key_user() throws Exception {
        User user = new User();
        user.setLogin("user1");
        user = userService.createUser(user, "123");

        for (int i = 0; i < 100; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
        }

        int assignedToUserCount = 20;
        for (int i = 0; i < assignedToUserCount; i++) {
            Network network = new Network();
            network.setName(namePrefix + randomUUID());
            Network created = networkService.create(network);
            assertThat(created.getId(), notNullValue());
            userService.assignNetwork(user.getId(), created.getId());
        }

        AccessKey accessKey = new AccessKey();
        AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);

        HivePrincipal principal = new HivePrincipal(accessKey);

        List<Network> networks = networkService.list(null, namePrefix + "%", null, true, 200, 0, principal);
        assertThat(networks, hasSize(assignedToUserCount));
    }

    @Test
    public void should_return_network_with_devices_and_device_classes_for_admin() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().getValue()));
        });
    }

    @Test
    public void should_return_null_network_if_user_is_not_an_admin() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, nullValue());
    }

    @Test
    public void should_return_network_for_if_client_is_assigned_to_it() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), created.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(user));
        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().getValue()));
        });
    }

    @Test
    public void should_return_network_with_devices_and_device_classes_for_admin_access_key() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKey accessKey = new AccessKey();
        AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().getValue()));
        });
    }

    @Test
    public void should_return_network_with_devices_and_device_classes_for_assigned_user_access_key() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKey accessKey = new AccessKey();
        AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), hasSize(5));
        returnedNetwork.getDevices().forEach(device -> {
            assertThat(device.getDeviceClass(), notNullValue());
            assertThat(device.getDeviceClass().getName(), equalTo(dc.getName().getValue()));
        });
    }

    @Test
    public void should_not_return_network_with_devices_and_device_classes_for_unassigned_user_access_key() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKey accessKey = new AccessKey();
        AccessKeyPermission permission = new AccessKeyPermission();
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, nullValue());
    }

    @Test
    public void should_return_network_without_devices_if_access_key_does_not_have_permissions() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.create(network);
        assertThat(created.getId(), notNullValue());
        userService.assignNetwork(user.getId(), network.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(randomUUID().toString()));
        dc.setVersion(new NullableWrapper<>("1"));
        for (int i = 0; i < 5; i++) {
            DeviceUpdate device = new DeviceUpdate();
            device.setName(new NullableWrapper<>(randomUUID().toString()));
            device.setGuid(new NullableWrapper<>(randomUUID().toString()));
            device.setKey(new NullableWrapper<>(randomUUID().toString()));
            device.setDeviceClass(new NullableWrapper<>(dc));
            device.setNetwork(new NullableWrapper<>(network));
            deviceService.deviceSave(device, Collections.emptySet());
        }

        AccessKey accessKey = new AccessKey();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setActions("do nothing");
        accessKey.setPermissions(Collections.singleton(permission));
        accessKey.setUser(user);
        HiveAuthentication authentication = new HiveAuthentication(new HivePrincipal(accessKey));
        authentication.setDetails(new HiveAuthentication.HiveAuthDetails(InetAddress.getByName("localhost"), "origin", "bearer"));

        Network returnedNetwork = networkService.getWithDevicesAndDeviceClasses(created.getId(), authentication);
        assertThat(returnedNetwork, notNullValue());
        assertThat(returnedNetwork.getDevices(), is(empty()));
    }

    @Test
    public void should_do_nothing_when_creates_or_verifies_network_if_network_is_null() throws Exception {
        assertThat(networkService.createOrVerifyNetwork(new NullableWrapper<>(null)), nullValue());
    }

    @Test
    public void should_throw_HiveException_if_id_provided_when_creates_or_verifies_network() throws Exception {
        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.INVALID_REQUEST_PARAMETERS);
        expectedException.expect(HiveExceptionMatcher.code(400));

        Network network = new Network();
        network.setId(-1L);
        networkService.createOrVerifyNetwork(new NullableWrapper<>(network));
    }

    @Test
    public void should_throw_HiveException_if_network_auto_creation_is_not_allowed_when_creates_or_verifies_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, false);

        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.NETWORK_AUTO_CREATE_NOT_ALLOWED);
        expectedException.expect(HiveExceptionMatcher.code(403));

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        networkService.createOrVerifyNetwork(new NullableWrapper<>(network));
    }

    @Test
    public void should_create_new_network_when_creates_or_verifies_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);

        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        Network created = networkService.createOrVerifyNetwork(new NullableWrapper<>(network));
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());
        assertThat(created.getName(), equalTo(network.getName()));
    }

    @Test
    public void should_verify_network_key_by_id_when_creates_or_verifies_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);
        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        Network created = networkService.createOrVerifyNetwork(new NullableWrapper<>(network));
        assertThat(created, notNullValue());

        Network verified = networkService.createOrVerifyNetwork(new NullableWrapper<>(created));
        assertThat(verified, notNullValue());
        assertThat(verified.getId(), equalTo(created.getId()));
        assertThat(verified.getName(), equalTo(created.getName()));
    }

    @Test
    public void should_verify_network_key_by_name_when_creates_or_verifies_network() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);
        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        Network created = networkService.createOrVerifyNetwork(new NullableWrapper<>(network));
        assertThat(created, notNullValue());

        Long networkId = created.getId();
        created.setId(null);

        Network verified = networkService.createOrVerifyNetwork(new NullableWrapper<>(created));
        assertThat(verified, notNullValue());
        assertThat(verified.getId(), equalTo(networkId));
        assertThat(verified.getName(), equalTo(created.getName()));
    }

    @Test
    public void should_throw_HiveException_when_creates_or_verifies_network_if_network_key_is_corrupted() throws Exception {
        configurationService.save(NetworkService.ALLOW_NETWORK_AUTO_CREATE, true);
        Network network = new Network();
        network.setName(namePrefix + randomUUID());
        network.setKey(randomUUID().toString());
        Network created = networkService.createOrVerifyNetwork(new NullableWrapper<>(network));
        assertThat(created, notNullValue());

        expectedException.expect(HiveException.class);
        expectedException.expectMessage(Messages.INVALID_NETWORK_KEY);
        expectedException.expect(HiveExceptionMatcher.code(403));

        created.setKey(randomUUID().toString());
        networkService.createOrVerifyNetwork(new NullableWrapper<>(created));
    }
}
