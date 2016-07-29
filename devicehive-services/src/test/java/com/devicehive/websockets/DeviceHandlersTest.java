package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketMethodTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.model.Equipment;
import com.devicehive.model.Network;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.NetworkVO;
import com.google.gson.JsonObject;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeviceHandlersTest extends AbstractWebSocketMethodTest {

    @Test
    public void should_save_device_with_key() throws Exception {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String deviceId = UUID.randomUUID().toString();
        String deviceKey = UUID.randomUUID().toString();
        DeviceUpdate device = DeviceFixture.createDevice(deviceKey);
        device.setDeviceClass(Optional.of(deviceClass));
        device.setNetwork(Optional.ofNullable(Network.convert(network)));

        //device/save
        JsonObject deviceSave = JsonFixture.createWsCommand("device/save", "1", deviceId, singletonMap("device", gson.toJsonTree(device)));

        String payload = runMethod(deviceSave, auth(ACCESS_KEY));
        JsonObject jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("device/save"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));


        //device/get
        JsonObject deviceGet = JsonFixture.createWsCommand("device/get", "2", deviceId);

        payload = runMethod(deviceGet, auth(ACCESS_KEY));
        jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("device/get"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));

        DeviceUpdate deviceResp = gson.fromJson(jsonResp.get("device").getAsJsonObject(), DeviceUpdate.class);
        assertThat(deviceResp.getGuid().orElse(null), is(deviceId));
        assertThat(deviceResp.getName(), is(device.getName()));
        assertThat(deviceResp.getStatus(), is(device.getStatus()));
        assertThat(deviceResp.getData().orElse(null), notNullValue());
        Network savedNetwork = deviceResp.getNetwork().orElse(null);
        assertThat(savedNetwork.getId(), notNullValue());
        assertThat(network.getName(), is(savedNetwork.getName()));
        assertThat(network.getDescription(), is(savedNetwork.getDescription()));
        DeviceClassUpdate savedClass = deviceResp.getDeviceClass().orElse(null);
        assertThat(savedClass, notNullValue());
        assertThat(savedClass.getId(), notNullValue());
        assertThat(savedClass.getName(), is(deviceClass.getName()));
        assertThat(savedClass.getPermanent(), is(deviceClass.getPermanent()));
        assertThat(savedClass.getOfflineTimeout(), is(deviceClass.getOfflineTimeout()));
        assertThat(savedClass.getData().orElse(null), notNullValue());
    }

    @Test
    public void should_save_device_as_admin() throws Exception {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String deviceId = UUID.randomUUID().toString();
        DeviceUpdate device = DeviceFixture.createDevice(deviceId);
        device.setDeviceClass(Optional.of(deviceClass));
        device.setNetwork(Optional.of(Network.convert(network)));

        //device/save
        JsonObject deviceSave = JsonFixture.createWsCommand("device/save", "1", deviceId, singletonMap("device", gson.toJsonTree(device)));

        String payload = runMethod(deviceSave, auth(ADMIN_LOGIN, ADMIN_PASS));
        JsonObject jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("device/save"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));


        //device/get
        JsonObject deviceGet = JsonFixture.createWsCommand("device/get", "2", deviceId);

        payload = runMethod(deviceGet, auth(ADMIN_LOGIN, ADMIN_PASS));
        jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("device/get"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));

        DeviceUpdate deviceResp = gson.fromJson(jsonResp.get("device").getAsJsonObject(), DeviceUpdate.class);
        assertThat(deviceResp.getGuid().get(), is(deviceId));
        assertThat(deviceResp.getName(), is(device.getName()));
        assertThat(deviceResp.getStatus(), is(device.getStatus()));
        assertThat(deviceResp.getData().get(), notNullValue());
        Network savedNetwork = deviceResp.getNetwork().get();
        assertThat(savedNetwork.getId(), notNullValue());
        assertThat(network.getName(), is(savedNetwork.getName()));
        assertThat(network.getDescription(), is(savedNetwork.getDescription()));
        DeviceClassUpdate savedClass = deviceResp.getDeviceClass().get();
        assertThat(savedClass, notNullValue());
        assertThat(savedClass.getId(), notNullValue());
        assertThat(savedClass.getName(), is(deviceClass.getName()));
        assertThat(savedClass.getPermanent(), is(deviceClass.getPermanent()));
        assertThat(savedClass.getOfflineTimeout(), is(deviceClass.getOfflineTimeout()));
        assertThat(savedClass.getData().get(), notNullValue());
    }

    @Test
    public void should_return_401_status_for_anonymous() throws Exception {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String deviceId = UUID.randomUUID().toString();
        String deviceKey = UUID.randomUUID().toString();
        DeviceUpdate device = DeviceFixture.createDevice(deviceKey);
        device.setDeviceClass(Optional.of(deviceClass));
        device.setNetwork(Optional.ofNullable(Network.convert(network)));

        //device/save
        JsonObject deviceSave = JsonFixture.createWsCommand("device/save", "1", deviceId, singletonMap("device", gson.toJsonTree(device)));

        String payload = runMethod(deviceSave, auth(ACCESS_KEY));
        JsonObject jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("device/save"));
        assertThat(jsonResp.get("requestId").getAsString(), is("1"));
        assertThat(jsonResp.get("status").getAsString(), is("success"));

        //device/get without deviceId/deviceKey authentication
        JsonObject deviceGet = JsonFixture.createWsCommand("device/get", "2");

        payload = runMethod(deviceGet, auth());
        jsonResp = gson.fromJson(payload, JsonObject.class);

        assertThat(jsonResp.get("action").getAsString(), is("device/get"));
        assertThat(jsonResp.get("requestId").getAsString(), is("2"));
        assertThat(jsonResp.get("status").getAsString(), is("error"));
        assertThat(jsonResp.get("code").getAsString(), is(String.valueOf(Response.Status.UNAUTHORIZED.getStatusCode())));
        assertThat(jsonResp.get("error").getAsString(), is(String.valueOf(Response.Status.UNAUTHORIZED.getReasonPhrase())));
    }

}
