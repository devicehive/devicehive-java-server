package com.devicehive.resource;

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
import static org.junit.Assert.assertTrue;

public class DeviceResourceTest extends AbstractResourceTest {

    @Test
    public void should_save_device_with_key() {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(deviceClass);
        deviceUpdate.setNetwork(network);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device
        DeviceVO device = performRequest("/device/" + guid, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, OK, DeviceVO.class);
        assertNotNull(device);
        assertThat(device.getGuid(), is(guid));
        assertThat(device.getName(), is(device.getName()));
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
        assertThat(savedClass.getData(), notNullValue());
    }

    @Test
    public void should_save_device_as_admin() {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(deviceClass);
        deviceUpdate.setNetwork(network);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device
        DeviceVO device = performRequest("/device/" + guid, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, OK, DeviceVO.class);
        assertNotNull(device);
        assertThat(device.getGuid(), is(guid));
        assertThat(device.getName(), is(device.getName()));
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
        assertThat(savedClass.getData(), notNullValue());
    }

    @Test
    public void should_return_401_status_for_anonymous() throws Exception {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(deviceClass);
        deviceUpdate.setNetwork(network);

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
        deviceClass.setEquipment(Collections.singleton(equipment));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(deviceClass);
        deviceUpdate.setNetwork(network);

        String login = RandomStringUtils.randomAlphabetic(10);
        String password = RandomStringUtils.randomAlphabetic(10);

        UserUpdate testUser = new UserUpdate();
        testUser.setLogin(login);
        testUser.setRole(UserRole.CLIENT.getValue());
        testUser.setPassword(password);
        testUser.setStatus(UserStatus.ACTIVE.getValue());

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
}
