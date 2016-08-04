package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Network;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.NetworkVO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;

public class DeviceCommandResourceTest extends AbstractResourceTest {

    @Test
    public void should_get_empty_response_with_status_204_when_command_not_processed() {
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

        // create command
        DeviceCommand command = DeviceFixture.createDeviceCommand();
        command = performRequest("/device/" + guid + "/command", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, CREATED, DeviceCommand.class);
        assertNotNull(command.getId());

        // try get not processed command
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 1);
        DeviceCommand updatedCommand = performRequest("/device/" + guid + "/command/" + command.getId() + "/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, NO_CONTENT, DeviceCommand.class);
        assertNull(updatedCommand);

    }

    @Test
    public void should_get_response_with_status_200_and_updated_command_when_command_was_processed_and_waitTimeout_is_0() {
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

        // create command
        DeviceCommand command = DeviceFixture.createDeviceCommand();
        command = performRequest("/device/" + guid + "/command", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, CREATED, DeviceCommand.class);
        assertNotNull(command.getId());

        //updateCommand
        response = performRequest("/device/" + guid + "/command/" + command.getId(), "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, NO_CONTENT, null);
        assertNotNull(response);

        // try get processed command
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 0);
        DeviceCommand updatedCommand = performRequest("/device/" + guid + "/command/" + command.getId() + "/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, OK, DeviceCommand.class);
        assertNotNull(updatedCommand);

    }

    @Test
    public void should_get_response_with_status_200_and_updated_command_when_command_was_processed_and_waitTimeout_is_0_and_polling_for_device() {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));
        DateTime timeStamp = new DateTime(DateTimeZone.UTC);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // create command
        DeviceCommand command = DeviceFixture.createDeviceCommand();
        command = performRequest("/device/" + guid + "/command", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, CREATED, DeviceCommand.class);
        assertNotNull(command.getId());

        // poll command
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 0);
        params.put("timestamp", timeStamp);
        ArrayList updatedCommands = new ArrayList();
        updatedCommands = performRequest("/device/" + guid + "/command/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), null, OK, updatedCommands.getClass());
        assertNotNull(updatedCommands);
        assertEquals(1, updatedCommands.size());

    }
}
