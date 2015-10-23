package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.GenericDAO;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.*;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class AccessKeyServiceTest extends AbstractResourceTest {

    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private UserService userService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private NetworkService networkService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private GenericDAO genericDAO;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_throw_IllegalParametersException_when_label_is_null() throws Exception {
        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.LABEL_IS_REQUIRED);
        accessKeyService.create(new User(), new AccessKey());
    }

    @Test
    public void should_throw_ActionNotAllowedException_when_access_key_already_exists() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        String label = RandomStringUtils.randomAlphabetic(10);
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(label);
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);
        assertThat(accessKey, notNullValue());

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.DUPLICATE_LABEL_FOUND);

        accessKey = new AccessKey();
        accessKey.setLabel(label);
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKeyService.create(user, accessKey);
    }

    @Test
    public void should_throw_IllegalParametersException_if_id_is_not_null_when_create() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKey accessKey = new AccessKey();
        accessKey.setId(-1L);
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.INVALID_REQUEST_PARAMETERS);
        accessKeyService.create(user, accessKey);
    }

    @Test
    public void should_create_access_key_for_user() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
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
        AccessKey created = accessKeyService.create(user, accessKey);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());
        assertThat(created.getKey(), equalTo(accessKey.getKey()));
        assertThat(created.getLabel(), equalTo(accessKey.getLabel()));
        assertThat(created.getType(), equalTo(accessKey.getType()));
        assertThat(created.getPermissions(), hasSize(1));
        AccessKeyPermission createdPermission = created.getPermissions().stream().findFirst().get();
        assertThat(createdPermission.getActionsAsSet(), hasSize(2));
        assertThat(createdPermission.getActionsAsSet(), hasItems(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue()));
        assertThat(createdPermission.getDeviceGuidsAsSet(), hasSize(3));
        assertThat(createdPermission.getDeviceGuidsAsSet(), hasItems("1", "2", "3"));
        assertThat(createdPermission.getDomainsAsSet(), hasSize(2));
        assertThat(createdPermission.getDomainsAsSet(), hasItems("domain1", "domain2"));
        assertThat(createdPermission.getNetworkIdsAsSet(), hasSize(2));
        assertThat(createdPermission.getNetworkIdsAsSet(), hasItems(1L, 2L));
        assertThat(createdPermission.getSubnetsAsSet(), hasSize(1));
    }

    @Test
    public void should_return_false_if_access_key_does_not_exist_when_update() throws Exception {
        assertFalse(
                accessKeyService.update(-1L, -1L, null));
    }

    @Test
    public void should_throw_IllegalParametersException_if_new_permissions_is_null() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyUpdate update = new AccessKeyUpdate();
        update.setLabel(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        update.setPermissions(new NullableWrapper<>(null));

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.INVALID_REQUEST_PARAMETERS);

        accessKeyService.update(user.getId(), accessKey.getId(), update);
    }

    @Test
    public void should_update_access_key() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyUpdate update = new AccessKeyUpdate();
        update.setLabel(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        update.setExpirationDate(new NullableWrapper<>(new Date(0)));
        update.setType(new NullableWrapper<>(AccessKeyType.SESSION.getValue()));
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setActionsArray(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue());
        permission.setDeviceGuidsCollection(Arrays.asList("1", "2", "3"));
        update.setPermissions(new NullableWrapper<>(singleton(permission)));

        assertTrue(
                accessKeyService.update(user.getId(), accessKey.getId(), update));
        AccessKey updated = accessKeyService.find(accessKey.getId(), user.getId());
        assertThat(updated.getLabel(), equalTo(update.getLabel().getValue()));
        assertThat(updated.getType(), equalTo(AccessKeyType.SESSION));
        assertThat(updated.getExpirationDate().getTime(), equalTo(update.getExpirationDate().getValue().getTime()));
        assertThat(updated.getPermissions(), hasSize(1));
        AccessKeyPermission updatedPerm = updated.getPermissions().stream().findFirst().get();
        assertThat(updatedPerm.getActionsAsSet(), hasSize(2));
        assertThat(updatedPerm.getActionsAsSet(), hasItems(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue()));
        assertThat(updatedPerm.getDeviceGuidsAsSet(), hasSize(3));
        assertThat(updatedPerm.getDeviceGuidsAsSet(), hasItems("1", "2", "3"));
    }

    @Test
    public void should_return_null_if_access_key_not_found_when_authenticate_by_key() throws Exception {
        assertNull(
                accessKeyService.authenticate(RandomStringUtils.randomAlphabetic(10)));
    }

    @Test
    public void should_expire_key_of_session_type_if_period_is_reached_when_authenticate_by_key() throws Exception {
        configurationService.save(Constants.SESSION_TIMEOUT, Constants.DEFAULT_SESSION_TIMEOUT);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKey accessKey = new AccessKey();
        accessKey.setKey(RandomStringUtils.random(20));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey.setType(AccessKeyType.SESSION);

        ZonedDateTime initialDate = LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault());
        accessKey.setExpirationDate(Date.from(initialDate.toInstant()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKey authenticated = accessKeyService.authenticate(accessKey.getKey());
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getId(), equalTo(accessKey.getId()));
        assertThat(authenticated.getExpirationDate(), notNullValue());
        ZonedDateTime changedDate = authenticated.getExpirationDate().toInstant().atZone(ZoneId.systemDefault());
        assertTrue(initialDate.isBefore(changedDate));
    }

    @Test
    public void should_not_expire_key_of_session_type__if_period_not_reached_when_authenticate_by_key() throws Exception {
        configurationService.save(Constants.SESSION_TIMEOUT, 0);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKey accessKey = new AccessKey();
        accessKey.setKey(RandomStringUtils.random(20));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey.setType(AccessKeyType.SESSION);

        Date initial = new Date();
        accessKey.setExpirationDate(initial);
        accessKey = accessKeyService.create(user, accessKey);

        AccessKey authenticated = accessKeyService.authenticate(accessKey.getKey());
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getId(), equalTo(accessKey.getId()));
        assertThat(authenticated.getExpirationDate(), notNullValue());
        assertThat(authenticated.getExpirationDate().getTime(), equalTo(initial.getTime()));
    }

    @Test
    public void should_just_return_access_key_if_access_key_is_not_of_session_type_when_authenticate() throws Exception {
        configurationService.save(Constants.SESSION_TIMEOUT, 0);

        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKey accessKey = new AccessKey();
        accessKey.setKey(RandomStringUtils.random(20));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey.setType(AccessKeyType.DEFAULT);
        accessKey = accessKeyService.create(user, accessKey);

        AccessKey authenticated = accessKeyService.authenticate(accessKey.getKey());
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getId(), equalTo(accessKey.getId()));
        assertThat(authenticated.getLabel(), equalTo(accessKey.getLabel()));
    }

    @Test
    public void should_create_external_access_key_for_admin() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        AccessKey accessKey = accessKeyService.authenticate(user);
        assertThat(accessKey, notNullValue());
        assertThat(accessKey.getId(), notNullValue());
        assertThat(accessKey.getLabel(), notNullValue());
        assertThat(accessKey.getKey(), notNullValue());
        assertThat(accessKey.getUser(), notNullValue());
        assertThat(accessKey.getUser().getId(), equalTo(user.getId()));
        assertThat(accessKey.getExpirationDate(), notNullValue());
        assertThat(accessKey.getType(), equalTo(AccessKeyType.SESSION));

        assertThat(accessKey.getPermissions(), hasSize(1));
        AccessKeyPermission permission = accessKey.getPermissions().stream().findFirst().get();
        assertThat(permission.getActions(), nullValue());
        assertThat(permission.getDeviceGuids(), nullValue());
        assertThat(permission.getNetworkIds(), nullValue());
        assertThat(permission.getDomains(), nullValue());
        assertThat(permission.getSubnets(), nullValue());
    }

    @Test
    public void should_create_external_access_key_for_client() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        AccessKey accessKey = accessKeyService.authenticate(user);
        assertThat(accessKey, notNullValue());
        assertThat(accessKey.getId(), notNullValue());
        assertThat(accessKey.getLabel(), notNullValue());
        assertThat(accessKey.getKey(), notNullValue());
        assertThat(accessKey.getUser(), notNullValue());
        assertThat(accessKey.getUser().getId(), equalTo(user.getId()));
        assertThat(accessKey.getExpirationDate(), notNullValue());
        assertThat(accessKey.getType(), equalTo(AccessKeyType.SESSION));

        assertThat(accessKey.getPermissions(), hasSize(1));
        AccessKeyPermission permission = accessKey.getPermissions().stream().findFirst().get();
        assertThat(permission.getActionsAsSet(), hasSize(AvailableActions.getClientActions().length));
        assertThat(permission.getActionsAsSet(), hasItems(AvailableActions.getClientActions()));
        assertThat(permission.getDeviceGuids(), nullValue());
        assertThat(permission.getNetworkIds(), nullValue());
        assertThat(permission.getDomains(), nullValue());
        assertThat(permission.getSubnets(), nullValue());
    }

    @Test
    public void should_return_access_key_by_user_id_and_key_id() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKey found = accessKeyService.find(accessKey.getId(), user.getId());
        assertThat(found, notNullValue());
        assertThat(found.getId(), equalTo(accessKey.getId()));
    }

    @Test
    public void should_check_network_access_permissions_for_client_null_permissions() throws Exception {
        User user = new User();
        user.setRole(UserRole.CLIENT);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);
        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);
        assertFalse(
                accessKeyService.hasAccessToNetwork(accessKey, network));
        userService.assignNetwork(user.getId(), network.getId());
        assertTrue(
                accessKeyService.hasAccessToNetwork(accessKey, network));
    }

    @Test
    public void should_check_network_access_permissions_for_admin_null_permissions() throws Exception {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);
        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);
        assertTrue(
                accessKeyService.hasAccessToNetwork(accessKey, network));
    }

    @Test
    public void should_check_network_access_permissions_for_client_with_permissions() throws Exception {
        User user = new User();
        user.setRole(UserRole.CLIENT);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);
        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setNetworkIdsCollection(singleton(-1L));
        Set<AccessKeyPermission> permissions = new HashSet<>();
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        accessKey = accessKeyService.create(user, accessKey);

        userService.assignNetwork(user.getId(), network.getId());
        assertFalse(
                accessKeyService.hasAccessToNetwork(accessKey, network));

        AccessKeyUpdate update = new AccessKeyUpdate();
        permission = new AccessKeyPermission();
        permission.setNetworkIdsCollection(singleton(network.getId()));
        update.setPermissions(new NullableWrapper<>(singleton(permission)));
        assertTrue(
                accessKeyService.update(user.getId(), accessKey.getId(), update));
        userService.unassignNetwork(user.getId(), network.getId());
        accessKey = accessKeyService.find(accessKey.getId(), user.getId());
        assertFalse(
                accessKeyService.hasAccessToNetwork(accessKey, network));

        userService.assignNetwork(user.getId(), network.getId());
        assertTrue(
                accessKeyService.hasAccessToNetwork(accessKey, network));
    }

    @Test
    public void should_create_access_key_from_oauth_grant() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, RandomStringUtils.random(10));

        OAuthClient client = new OAuthClient();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));

        OAuthGrant grant = new OAuthGrant();
        grant.setClient(client);
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant.setAccessType(AccessType.ONLINE);

        AccessKey key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, new Date());
        assertThat(key, notNullValue());
        assertThat(key.getLabel(), notNullValue());
        assertThat(key.getKey(), notNullValue());
        assertThat(key.getPermissions(), notNullValue());
        assertThat(key.getPermissions(), hasSize(1));
        assertThat(key.getExpirationDate(), notNullValue());
        AccessKeyPermission permission = key.getPermissions().stream().findFirst().get();
        assertThat(permission.getActionsAsSet(), hasSize(1));
        assertThat(permission.getActionsAsSet(), hasItem(AccessKeyAction.GET_DEVICE.getValue()));
        assertThat(permission.getDomainsAsSet(), hasSize(1));
        assertThat(permission.getDomainsAsSet(), hasItem(client.getDomain()));
    }

    @Test
    public void should_update_access_key_from_oauth_grant() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, RandomStringUtils.random(10));

        OAuthClient client = new OAuthClient();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));
        OAuthGrant grant = new OAuthGrant();
        grant.setClient(client);
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant.setAccessType(AccessType.ONLINE);
        AccessKey created = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, new Date());

        OAuthClient newClient = new OAuthClient();
        newClient.setName(RandomStringUtils.randomAlphabetic(10));
        newClient.setDomain(RandomStringUtils.randomAlphabetic(10));
        OAuthGrant newGrant = new OAuthGrant();
        newGrant.setAccessKey(created);
        newGrant.setClient(newClient);
        newGrant.setScope(AccessKeyAction.MANAGE_ACCESS_KEY.getValue());
        newGrant.setAccessType(AccessType.ONLINE);

        AccessKey updated = accessKeyService.updateAccessKeyFromOAuthGrant(newGrant, user, new Date());
        assertThat(updated.getId(), equalTo(created.getId()));
        assertThat(updated.getLabel(), notNullValue());
        assertThat(updated.getKey(), notNullValue());
        assertThat(updated.getPermissions(), notNullValue());
        assertThat(updated.getPermissions(), hasSize(1));
        assertThat(updated.getExpirationDate(), notNullValue());
        AccessKeyPermission permission = updated.getPermissions().stream().findFirst().get();
        assertThat(permission.getActionsAsSet(), hasSize(1));
        assertThat(permission.getActionsAsSet(), hasItem(AccessKeyAction.MANAGE_ACCESS_KEY.getValue()));
        assertThat(permission.getDomainsAsSet(), hasSize(1));
        assertThat(permission.getDomainsAsSet(), hasItem(newClient.getDomain()));
    }

    @Test
    public void should_check_device_access_permissions_and_return_true_for_admin() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, RandomStringUtils.random(10));

        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        device.setName(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        device.setNetwork(new NullableWrapper<>(network));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(new NullableWrapper<>("0.1"));
        device.setDeviceClass(new NullableWrapper<>(dc));
        deviceService.deviceSave(device, emptySet());

        userService.assignNetwork(user.getId(), network.getId());

        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermission()));
        accessKey = accessKeyService.create(user, accessKey);

        assertTrue(accessKeyService.hasAccessToDevice(accessKey, device.getGuid().getValue()));
    }

    @Test
    public void should_check_device_access_permissions_and_return_true_for_client() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, RandomStringUtils.random(10));

        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        device.setName(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        device.setNetwork(new NullableWrapper<>(network));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(new NullableWrapper<>("0.1"));
        device.setDeviceClass(new NullableWrapper<>(dc));
        deviceService.deviceSave(device, emptySet());

        userService.assignNetwork(user.getId(), network.getId());

        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setDeviceGuidsCollection(Arrays.asList(device.getGuid().getValue()));
        accessKey.setPermissions(singleton(permission));
        accessKey = accessKeyService.create(user, accessKey);

        assertTrue(accessKeyService.hasAccessToDevice(accessKey, device.getGuid().getValue()));
    }

    @Test
    public void should_check_device_access_permissions_and_return_false() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, RandomStringUtils.random(10));

        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        device.setName(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        device.setNetwork(new NullableWrapper<>(network));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(new NullableWrapper<>("0.1"));
        device.setDeviceClass(new NullableWrapper<>(dc));
        deviceService.deviceSave(device, emptySet());

        AccessKey accessKey = new AccessKey();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setDeviceGuids(null);
        accessKey.setPermissions(singleton(permission));
        accessKey = accessKeyService.create(user, accessKey);

        assertFalse(accessKeyService.hasAccessToDevice(accessKey, device.getGuid().getValue()));
    }

    @Ignore
    @Test
    public void should_list_access_keys_by_user() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, RandomStringUtils.random(10));

        for (int i = 0; i < 50; i++) {
            AccessKey accessKey = new AccessKey();
            accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
            accessKey.setPermissions(singleton(new AccessKeyPermission()));
            accessKey.setType(AccessKeyType.DEFAULT);
            accessKeyService.create(user, accessKey);
        }

//        List<AccessKey> k = genericDAO.createQuery("select ak from AccessKey ak left join fetch ak.user u where u.id = :user", AccessKey.class)
//            .setParameter("user", user.getId())
//            .getResultList();

        List<AccessKey> keys = accessKeyService.list(user.getId(), null, null, null, null, false, 0, 100);
        assertThat(keys, hasSize(50));
    }
}
