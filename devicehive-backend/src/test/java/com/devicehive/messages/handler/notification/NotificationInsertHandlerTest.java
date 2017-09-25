package com.devicehive.messages.handler.notification;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

public class NotificationInsertHandlerTest extends AbstractSpringTest {

    @Autowired
    private HazelcastService hazelcastService;

    private NotificationInsertHandler handler;

    private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = Mockito.mock(EventBus.class);

        handler = new NotificationInsertHandler();
        handler.setEventBus(eventBus);
        handler.setHazelcastService(hazelcastService);
    }

    @Test
    public void testInsertNotification() throws ExecutionException, InterruptedException, TimeoutException {
        final String deviceId = UUID.randomUUID().toString();
        final long id = System.nanoTime();

        DeviceNotification originalNotification = new DeviceNotification();
        originalNotification.setTimestamp(Date.from(Instant.now()));
        originalNotification.setId(id);
        originalNotification.setDeviceId(deviceId);
        originalNotification.setNotification("SOME TEST DATA");
        originalNotification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));
        NotificationInsertRequest nir = new NotificationInsertRequest(originalNotification);
        Response response = handler.handle(
                Request.newBuilder()
                        .withBody(nir)
                        .build()
        );

        assertTrue(hazelcastService.find(id, deviceId, DeviceNotification.class)
                .filter(notification -> notification.equals(originalNotification))
                .isPresent());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        NotificationEvent event = eventCaptor.getValue();
        assertEquals(event.getNotification(), originalNotification);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof NotificationInsertResponse);
        NotificationInsertResponse body = (NotificationInsertResponse) response.getBody();
        assertEquals(body.getDeviceNotification(), originalNotification);
    }
}
