package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
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
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceNotificationResourceTest extends AbstractResourceTest {

    @Test
    public void should_get_response_with_status_200_and_notification_when_waitTimeout_is_0_and_polling_for_device() {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(Network.convert(network)));
        DateTime timeStamp = new DateTime(DateTimeZone.UTC);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // When creating device, automatically created one notification.

        // poll notification
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 0);
        params.put("timestamp", timeStamp);
        ArrayList notifications = new ArrayList();
        notifications = performRequest("/device/" + guid + "/notification/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), null, OK, notifications.getClass());
        assertNotNull(notifications);
        assertEquals(1, notifications.size());


    }
}
