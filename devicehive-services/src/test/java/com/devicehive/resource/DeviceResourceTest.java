package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.*;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.vo.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class DeviceResourceTest extends AbstractResourceTest {

    @Test
    public void should_save_device_with_key() {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device
        DeviceVO device = performRequest("/device/" + guid, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, OK, DeviceVO.class);
        assertNotNull(device);
        assertThat(device.getGuid(), is(guid));
        assertThat(device.getName(), is(device.getName()));
        assertThat(device.getStatus(), is(device.getStatus()));
        assertThat(device.getData(), notNullValue());
        NetworkVO savedNetwork = device.getNetwork();
        assertThat(savedNetwork.getId(), notNullValue());
        assertThat(network.getName(), is(savedNetwork.getName()));
        assertThat(network.getDescription(), is(savedNetwork.getDescription()));
        DeviceClassVO savedClass = device.getDeviceClass();
        assertThat(savedClass, notNullValue());
        assertThat(savedClass.getId(), notNullValue());
        assertThat(savedClass.getName(), is(deviceClass.getName().get()));
        assertThat(savedClass.getIsPermanent(), is(deviceClass.getPermanent().get()));
        assertThat(savedClass.getOfflineTimeout(), is(deviceClass.getOfflineTimeout().get()));
        assertThat(savedClass.getData(), notNullValue());
    }

    @Test
    public void should_save_device_as_admin() {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device
        DeviceVO device = performRequest("/device/" + guid, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, OK, DeviceVO.class);
        assertNotNull(device);
        assertThat(device.getGuid(), is(guid));
        assertThat(device.getName(), is(device.getName()));
        assertThat(device.getStatus(), is(device.getStatus()));
        assertThat(device.getData(), notNullValue());
        NetworkVO savedNetwork = device.getNetwork();
        assertThat(savedNetwork.getId(), notNullValue());
        assertThat(network.getName(), is(savedNetwork.getName()));
        assertThat(network.getDescription(), is(savedNetwork.getDescription()));
        DeviceClassVO savedClass = device.getDeviceClass();
        assertThat(savedClass, notNullValue());
        assertThat(savedClass.getId(), notNullValue());
        assertThat(savedClass.getName(), is(deviceClass.getName().get()));
        assertThat(savedClass.getIsPermanent(), is(deviceClass.getPermanent().get()));
        assertThat(savedClass.getOfflineTimeout(), is(deviceClass.getOfflineTimeout().get()));
        assertThat(savedClass.getData(), notNullValue());
    }

    @Test
    public void should_return_401_status_for_anonymous() throws Exception {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device without authentication
        response = performRequest("/device/" + guid, "GET", emptyMap(), emptyMap(), deviceUpdate, UNAUTHORIZED, null);
        assertNotNull(response);
    }

    @Test
    public void should_return_403_for_basic_authorized_user_that_has_no_access_to_device() throws Exception {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        String login = RandomStringUtils.randomAlphabetic(10);
        String password = RandomStringUtils.randomAlphabetic(10);

        UserUpdate testUser = new UserUpdate();
        testUser.setLogin(Optional.of(login));
        testUser.setRole(Optional.ofNullable(UserRole.CLIENT.getValue()));
        testUser.setPassword(Optional.of(password));
        testUser.setStatus(Optional.ofNullable(UserStatus.ACTIVE.getValue()));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        //register user
        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), testUser, CREATED, UserVO.class);
        assertThat(user.getId(), Matchers.notNullValue());

        //testing that user has no access to device
        response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(login, password)), deviceUpdate, FORBIDDEN, null);
        assertNotNull(response);
    }

    @Test
    public void should_return_403_for_key_authorized_user_that_has_no_access_to_device() throws Exception {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        String login = RandomStringUtils.randomAlphabetic(10);
        String password = RandomStringUtils.randomAlphabetic(10);

        UserUpdate testUser = new UserUpdate();
        testUser.setLogin(Optional.ofNullable(login));
        testUser.setRole(Optional.ofNullable(UserRole.CLIENT.getValue()));
        testUser.setPassword(Optional.ofNullable(password));
        testUser.setStatus(Optional.ofNullable(UserStatus.ACTIVE.getValue()));

        //create user
        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), testUser, CREATED, UserVO.class);
        assertThat(user.getId(), Matchers.notNullValue());

        //create network
        //todo: check if it is necessary to set up user
        //network.setUsers(singleton(user));
        performRequest("/network", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), network, CREATED, NetworkVO.class);
        assertThat(user.getId(), Matchers.notNullValue());

        //register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        AccessKeyVO key = new AccessKeyVO();
        key.setType(AccessKeyType.DEFAULT);
        key.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray(AvailableActions.getClientActions());
        permission.setDeviceGuidsCollection(singleton("9999999"));
        key.setPermissions(singleton(permission));
        UserVO usr = new UserVO();
        usr.setId(user.getId());
        key.setUser(usr);

        //Create key for user
        key = performRequest("/user/current/accesskey", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(login, password)), key, CREATED, AccessKeyVO.class);
        assertThat(key.getId(), Matchers.notNullValue());

        //testing that user has no access to device
        response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(key.getKey())), deviceUpdate, FORBIDDEN, null);
        assertNotNull(response);
    }

    @Test
    public void should_return_403_for_key_authorized_user_that_has_no_access_to_network() throws Exception {

        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        String login = RandomStringUtils.randomAlphabetic(10);
        String password = RandomStringUtils.randomAlphabetic(10);

        UserUpdate testUser = new UserUpdate();
        testUser.setLogin(Optional.of(login));
        testUser.setRole(Optional.ofNullable(UserRole.CLIENT.getValue()));
        testUser.setPassword(Optional.of(password));
        testUser.setStatus(Optional.ofNullable(UserStatus.ACTIVE.getValue()));

        //create user
        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), testUser, CREATED, UserVO.class);
        assertThat(user.getId(), Matchers.notNullValue());

        //create network
        network = performRequest("/network", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), network, CREATED, NetworkVO.class);
        assertThat(network.getId(), Matchers.notNullValue());

        //register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        AccessKeyVO key = new AccessKeyVO();
        key.setType(AccessKeyType.DEFAULT);
        key.setLabel(RandomStringUtils.randomAlphabetic(10));
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setActionsArray(AvailableActions.getClientActions());
        permission.setDeviceGuidsCollection(singleton("9999999"));
        key.setPermissions(singleton(permission));
        UserVO usr = new UserVO();
        usr.setId(user.getId());
        key.setUser(usr);

        //Create key for user
        key = performRequest("/user/current/accesskey", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(login, password)), key, CREATED, AccessKeyVO.class);
        assertThat(key.getId(), Matchers.notNullValue());

        //testing that user has no access to network
        response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(key.getKey())), deviceUpdate, FORBIDDEN, null);
        assertNotNull(response);
    }
}
