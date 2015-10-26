package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.Network;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class DeviceResourceTest extends AbstractResourceTest {

    @Test
    public void should_save_device_with_key() {
        Equipment equipment = DeviceFixture.createEquipment();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        Network network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device
        Device device = performRequest("/device/" + guid, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, OK, Device.class);
        assertNotNull(device);
        assertThat(device.getGuid(), is(guid));
        assertThat(device.getName(), is(device.getName()));
        assertThat(device.getStatus(), is(device.getStatus()));
        assertThat(device.getData(), notNullValue());
        Network savedNetwork = device.getNetwork();
        assertThat(savedNetwork.getId(), notNullValue());
        assertThat(network.getName(), is(savedNetwork.getName()));
        assertThat(network.getDescription(), is(savedNetwork.getDescription()));
        DeviceClass savedClass = device.getDeviceClass();
        assertThat(savedClass, notNullValue());
        assertThat(savedClass.getId(), notNullValue());
        assertThat(savedClass.getName(), is(deviceClass.getName().get()));
        assertThat(savedClass.getVersion(), is(deviceClass.getVersion().get()));
        assertThat(savedClass.getPermanent(), is(deviceClass.getPermanent().get()));
        assertThat(savedClass.getOfflineTimeout(), is(deviceClass.getOfflineTimeout().get()));
        assertThat(savedClass.getData(), notNullValue());
    }

    @Test
    public void should_save_device_as_admin() {
        Equipment equipment = DeviceFixture.createEquipment();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        Network network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // get device
        Device device = performRequest("/device/" + guid, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), deviceUpdate, OK, Device.class);
        assertNotNull(device);
        assertThat(device.getGuid(), is(guid));
        assertThat(device.getName(), is(device.getName()));
        assertThat(device.getStatus(), is(device.getStatus()));
        assertThat(device.getData(), notNullValue());
        Network savedNetwork = device.getNetwork();
        assertThat(savedNetwork.getId(), notNullValue());
        assertThat(network.getName(), is(savedNetwork.getName()));
        assertThat(network.getDescription(), is(savedNetwork.getDescription()));
        DeviceClass savedClass = device.getDeviceClass();
        assertThat(savedClass, notNullValue());
        assertThat(savedClass.getId(), notNullValue());
        assertThat(savedClass.getName(), is(deviceClass.getName().get()));
        assertThat(savedClass.getVersion(), is(deviceClass.getVersion().get()));
        assertThat(savedClass.getPermanent(), is(deviceClass.getPermanent().get()));
        assertThat(savedClass.getOfflineTimeout(), is(deviceClass.getOfflineTimeout().get()));
        assertThat(savedClass.getData(), notNullValue());
    }

    @Test
    public void should_return_401_status_for_anonymous() throws Exception {
        Equipment equipment = DeviceFixture.createEquipment();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        Network network = DeviceFixture.createNetwork();
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
}
