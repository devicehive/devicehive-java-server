package com.devicehive.websockets;

import com.devicehive.base.AbstractWebSocketTest;
import com.devicehive.base.SynchronousWebSocketClientHandler;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.base.fixture.JsonFixture;
import com.devicehive.model.Equipment;
import com.devicehive.model.Network;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;

import java.util.Collections;
import java.util.UUID;

public class DeviceHandlersTest extends AbstractWebSocketTest {

    @Test
    public void should_save_device() throws Exception {
        SynchronousWebSocketClientHandler handler = syncClientHandler("/websocket/device");

        Equipment equipment = DeviceFixture.createEquipment();

        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(new NullableWrapper<>(Collections.singleton(equipment)));

        Network network = DeviceFixture.createNetwork();

        String deviceId = UUID.randomUUID().toString();
        String deviceKey = UUID.randomUUID().toString();

        DeviceUpdate device = DeviceFixture.createDevice(deviceKey);
        device.setDeviceClass(new NullableWrapper<>(deviceClass));
        device.setNetwork(new NullableWrapper<>(network));
        JsonObject root = JsonFixture.createWsCommand("device/save", "1", deviceId, deviceKey, Pair.of("device", gson.toJsonTree(device)));

        handler.sendMessage(new TextMessage(gson.toJson(root)));
        TextMessage response = handler.awaitMessage(WAIT_TIMEOUT);
        System.out.println(response);
    }
}
