package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.model.rpc.ListAccessKeyRequest;
import com.devicehive.model.rpc.ListAccessKeyResponse;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

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
    private AccessKeyDao accessKeyDao;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_throw_IllegalParametersException_when_label_is_null() throws Exception {
        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.LABEL_IS_REQUIRED);
        accessKeyService.create(new UserVO(), new AccessKeyVO());
    }

    @Test
    public void should_throw_ActionNotAllowedException_when_access_key_already_exists() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        String label = RandomStringUtils.randomAlphabetic(10);
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(label);
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);
        assertThat(accessKey, notNullValue());

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.DUPLICATE_LABEL_FOUND);

        accessKey = new AccessKeyVO();
        accessKey.setLabel(label);
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKeyService.create(user, accessKey);
    }

    @Test
    public void should_throw_IllegalParametersException_if_id_is_not_null_when_create() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setId(-1L);
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.INVALID_REQUEST_PARAMETERS);
        accessKeyService.create(user, accessKey);
    }

    @Test
    public void should_create_access_key_for_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setKey(RandomStringUtils.randomAlphabetic(10));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setType(AccessKeyType.SESSION);

        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue());
        permission.setDeviceGuidsCollection(Arrays.asList("1", "2", "3"));
        permission.setDomainArray("domain1", "domain2");
        permission.setNetworkIdsCollection(Arrays.asList(1L, 2L));
        permission.setSubnetsArray("localhost");
        accessKey.setPermissions(singleton(permission));

        AccessKeyVO created = accessKeyService.create(user, accessKey);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());
        assertThat(created.getKey(), equalTo(accessKey.getKey()));
        assertThat(created.getLabel(), equalTo(accessKey.getLabel()));
        assertThat(created.getType(), equalTo(accessKey.getType()));
        assertThat(created.getPermissions(), hasSize(1));

        AccessKeyPermissionVO createdPermission = created.getPermissions().stream().findFirst().get();
        assertThat(createdPermission.getActionsAsSet(), hasSize(2));
        assertThat(createdPermission.getActionsAsSet(), Matchers.hasItems(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue()));
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
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyUpdate update = new AccessKeyUpdate();
        update.setLabel(Optional.of(RandomStringUtils.randomAlphabetic(10)));
        update.setPermissions(Optional.ofNullable(null));

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.INVALID_REQUEST_PARAMETERS);

        accessKeyService.update(user.getId(), accessKey.getId(), update);
    }

    @Test
    public void should_update_access_key() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyUpdate update = new AccessKeyUpdate();
        update.setLabel(Optional.of(RandomStringUtils.randomAlphabetic(10)));
        update.setExpirationDate(Optional.of(new Date(0)));
        update.setType(Optional.of(AccessKeyType.SESSION.getValue()));
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue());
        permission.setDeviceGuidsCollection(Arrays.asList("1", "2", "3"));
        update.setPermissions(Optional.of(singleton(permission)));

        assertTrue(accessKeyService.update(user.getId(), accessKey.getId(), update));
        AccessKeyVO updated = accessKeyService.find(accessKey.getId(), user.getId());
        assertThat(updated.getLabel(), equalTo(update.getLabel().get()));
        assertThat(updated.getType(), equalTo(AccessKeyType.SESSION));
        assertThat(updated.getExpirationDate().getTime(), equalTo(update.getExpirationDate().get().getTime()));
        assertThat(updated.getPermissions(), hasSize(1));
        AccessKeyPermissionVO updatedPerm = updated.getPermissions().stream().findFirst().get();
        assertThat(updatedPerm.getActionsAsSet(), hasSize(2));
        assertThat(updatedPerm.getActionsAsSet(), Matchers.hasItems(AccessKeyAction.GET_DEVICE.getValue(), AccessKeyAction.GET_DEVICE_COMMAND.getValue()));
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

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setKey(RandomStringUtils.random(20));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey.setType(AccessKeyType.SESSION);

        ZonedDateTime initialDate = LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault());
        accessKey.setExpirationDate(Date.from(initialDate.toInstant()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyVO authenticated = accessKeyService.authenticate(accessKey.getKey());
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getId(), equalTo(accessKey.getId()));
        assertThat(authenticated.getExpirationDate(), notNullValue());
        ZonedDateTime changedDate = authenticated.getExpirationDate().toInstant().atZone(ZoneId.systemDefault());
        assertTrue(initialDate.isBefore(changedDate));
    }

    @Test
    public void should_not_expire_key_of_session_type__if_period_not_reached_when_authenticate_by_key() throws Exception {
        configurationService.save(Constants.SESSION_TIMEOUT, 0);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setKey(RandomStringUtils.random(20));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey.setType(AccessKeyType.SESSION);

        Date initial = timestampService.getDate();
        accessKey.setExpirationDate(initial);
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyVO authenticated = accessKeyService.authenticate(accessKey.getKey());
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getId(), equalTo(accessKey.getId()));
        assertThat(authenticated.getExpirationDate(), notNullValue());
        assertThat(authenticated.getExpirationDate().getTime(), equalTo(initial.getTime()));
    }

    @Test
    public void should_just_return_access_key_if_access_key_is_not_of_session_type_when_authenticate() throws Exception {
        configurationService.save(Constants.SESSION_TIMEOUT, 0);

        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setKey(RandomStringUtils.random(20));
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey.setType(AccessKeyType.DEFAULT);
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyVO authenticated = accessKeyService.authenticate(accessKey.getKey());
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getId(), equalTo(accessKey.getId()));
        assertThat(authenticated.getLabel(), equalTo(accessKey.getLabel()));
    }

    @Test
    public void should_return_access_key_by_user_id_and_key_id() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);

        AccessKeyVO found = accessKeyService.find(accessKey.getId(), user.getId());
        assertThat(found, notNullValue());
        assertThat(found.getId(), equalTo(accessKey.getId()));
    }

    @Test
    public void should_check_network_access_permissions_for_client_null_permissions() throws Exception {
        UserVO user = new UserVO();
        user.setRole(UserRole.CLIENT);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);
        NetworkVO network = new NetworkVO();
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
        UserVO user = new UserVO();
        user.setRole(UserRole.ADMIN);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);
        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);
        assertTrue(
                accessKeyService.hasAccessToNetwork(accessKey, network));
    }

    @Test
    public void should_check_network_access_permissions_for_client_with_permissions() throws Exception {
        UserVO user = new UserVO();
        user.setRole(UserRole.CLIENT);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);
        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setNetworkIdsCollection(singleton(-1L));
        Set<AccessKeyPermissionVO> permissions = new HashSet<>();
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        accessKey = accessKeyService.create(user, accessKey);

        userService.assignNetwork(user.getId(), network.getId());
        assertFalse(accessKeyService.hasAccessToNetwork(accessKey, network));

        AccessKeyUpdate update = new AccessKeyUpdate();
        permission = new AccessKeyPermissionVO();
        permission.setNetworkIdsCollection(singleton(network.getId()));
        update.setPermissions(Optional.of(singleton(permission)));
        assertTrue(accessKeyService.update(user.getId(), accessKey.getId(), update));
        userService.unassignNetwork(user.getId(), network.getId());
        accessKey = accessKeyService.find(accessKey.getId(), user.getId());
        boolean keyHasAccessToNetwork = accessKeyService.hasAccessToNetwork(accessKey, network);
        assertFalse(keyHasAccessToNetwork);

        userService.assignNetwork(user.getId(), network.getId());
        keyHasAccessToNetwork = accessKeyService.hasAccessToNetwork(accessKey, network);
        assertTrue(keyHasAccessToNetwork);
    }

    @Test
    public void should_create_access_key_from_oauth_grant() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, RandomStringUtils.random(10));

        OAuthClientVO client = new OAuthClientVO();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));

        OAuthGrantVO grant = new OAuthGrantVO();
        grant.setClient(client);
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant.setAccessType(AccessType.ONLINE);

        AccessKeyVO key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, timestampService.getDate());
        assertThat(key, notNullValue());
        assertThat(key.getLabel(), notNullValue());
        assertThat(key.getKey(), notNullValue());
        assertThat(key.getPermissions(), notNullValue());
        assertThat(key.getPermissions(), hasSize(1));
        assertThat(key.getExpirationDate(), notNullValue());
        AccessKeyPermissionVO permission = key.getPermissions().stream().findFirst().get();
        assertThat(permission.getActionsAsSet(), hasSize(1));
        assertThat(permission.getActionsAsSet(), Matchers.hasItem(AccessKeyAction.GET_DEVICE.getValue()));
        assertThat(permission.getDomainsAsSet(), hasSize(1));
        assertThat(permission.getDomainsAsSet(), hasItem(client.getDomain()));
    }

    @Test
    public void should_update_access_key_from_oauth_grant() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, RandomStringUtils.random(10));

        OAuthClientVO client = new OAuthClientVO();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));
        OAuthGrantVO grant = new OAuthGrantVO();
        grant.setClient(client);
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant.setAccessType(AccessType.ONLINE);
        AccessKeyVO created = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, timestampService.getDate());

        OAuthClientVO newClient = new OAuthClientVO();
        newClient.setName(RandomStringUtils.randomAlphabetic(10));
        newClient.setDomain(RandomStringUtils.randomAlphabetic(10));
        OAuthGrantVO newGrant = new OAuthGrantVO();
        newGrant.setAccessKey(created);
        newGrant.setClient(newClient);
        newGrant.setScope(AccessKeyAction.MANAGE_ACCESS_KEY.getValue());
        newGrant.setAccessType(AccessType.ONLINE);

        AccessKeyVO updated = accessKeyService.updateAccessKeyFromOAuthGrant(newGrant, user, timestampService.getDate());
        assertThat(updated.getId(), equalTo(created.getId()));
        assertThat(updated.getLabel(), notNullValue());
        assertThat(updated.getKey(), notNullValue());
        assertThat(updated.getPermissions(), notNullValue());
        assertThat(updated.getPermissions(), hasSize(1));
        assertThat(updated.getExpirationDate(), notNullValue());
        AccessKeyPermissionVO permission = updated.getPermissions().stream().findFirst().get();
        assertThat(permission.getActionsAsSet(), hasSize(1));
        assertThat(permission.getActionsAsSet(), Matchers.hasItem(AccessKeyAction.MANAGE_ACCESS_KEY.getValue()));
        assertThat(permission.getDomainsAsSet(), hasSize(1));
        assertThat(permission.getDomainsAsSet(), hasItem(newClient.getDomain()));
    }

    @Test
    public void should_check_device_access_permissions_and_return_true_for_admin() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, RandomStringUtils.random(10));

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setNetwork(Optional.ofNullable(network));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(device, emptySet());

        userService.assignNetwork(user.getId(), network.getId());

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
        accessKey = accessKeyService.create(user, accessKey);

        assertTrue(accessKeyService.hasAccessToDevice(accessKey, device.getGuid().orElse(null)));
    }

    @Test
    public void should_check_device_access_permissions_and_return_true_for_client() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, RandomStringUtils.random(10));

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setNetwork(Optional.ofNullable(network));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(device, emptySet());

        userService.assignNetwork(user.getId(), network.getId());

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setDeviceGuidsCollection(Arrays.asList(device.getGuid().orElse(null)));
        accessKey.setPermissions(singleton(permission));
        accessKey = accessKeyService.create(user, accessKey);

        assertTrue(accessKeyService.hasAccessToDevice(accessKey, device.getGuid().orElse(null)));
    }

    @Test
    public void should_check_device_access_permissions_and_return_false() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, RandomStringUtils.random(10));

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setNetwork(Optional.ofNullable(network));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        device.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(device, emptySet());

        AccessKeyVO accessKey = new AccessKeyVO();
        accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setDeviceGuids(null);
        accessKey.setPermissions(singleton(permission));
        accessKey = accessKeyService.create(user, accessKey);

        assertFalse(accessKeyService.hasAccessToDevice(accessKey, device.getGuid().orElse(null)));
    }

    @Test
    public void should_list_access_keys_by_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, RandomStringUtils.random(10));

        for (int i = 0; i < 50; i++) {
            AccessKeyVO accessKey = new AccessKeyVO();
            accessKey.setLabel(RandomStringUtils.randomAlphabetic(10));
            accessKey.setPermissions(singleton(new AccessKeyPermissionVO()));
            accessKey.setType(AccessKeyType.DEFAULT);
            accessKeyService.create(user, accessKey);
        }

        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            ListAccessKeyRequest req = request.getBody().cast(ListAccessKeyRequest.class);
            final List<AccessKeyVO> accessKeys =
                    accessKeyDao.list(req.getUserId(), req.getLabel(),
                            req.getLabelPattern(), req.getType(),
                            req.getSortField(), req.getSortOrderAsc(),
                            req.getTake(), req.getSkip());

            return Response.newBuilder()
                    .withBody(new ListAccessKeyResponse(accessKeys))
                    .buildSuccess();
        });

        accessKeyService.list(user.getId(), null, null, AccessKeyType.DEFAULT.getValue(), null, false, 50, 0)
                .thenAccept(accessKeys -> {
                    assertThat(accessKeys, not(empty()));
                    assertThat(accessKeys, hasSize(50));
                }).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }
}
