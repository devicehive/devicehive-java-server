package com.devicehive.service;

/*
 * #%L
 * DeviceHive Frontend Logic
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
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDao;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.rpc.*;
import com.devicehive.service.exception.BackendException;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class DeviceNotificationServiceTest extends AbstractResourceTest {

    @Autowired
    private DeviceNotificationService notificationService;

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

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testFindOneWithResponse() throws Exception {
        final String guid = UUID.randomUUID().toString();
        final long id = System.currentTimeMillis();
        final String notification = "Expected notification";
        final Date timestamp = new Date();
        final String parameters = "{\"param1\":\"value1\",\"param2\":\"value2\"}";

        final DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(id);
        deviceNotification.setDeviceGuid(guid);
        deviceNotification.setNotification(notification);
        deviceNotification.setTimestamp(timestamp);
        deviceNotification.setParameters(new JsonStringWrapper(parameters));

        // return response for any request
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationSearchResponse(Collections.singletonList(deviceNotification)))
                .buildSuccess());

        // call service method
        notificationService.findOne(id, guid)
                .thenAccept(opt -> {
                    assertTrue(opt.isPresent());
                    assertEquals(guid, opt.get().getDeviceGuid());
                    assertEquals(timestamp, opt.get().getTimestamp());
                    assertEquals(parameters, opt.get().getParameters().getJsonString());
                    assertEquals(notification, opt.get().getNotification());
                    assertEquals(Long.valueOf(id), opt.get().getId());
                })
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
        assertEquals(Action.NOTIFICATION_SEARCH_REQUEST.name(), argument.getValue().getBody().getAction());

        NotificationSearchRequest request = argument.getValue().getBody().cast(NotificationSearchRequest.class);
        assertEquals(id, request.getId().longValue());
        assertEquals(guid, request.getGuid());
        assertNull(request.getStatus());
        assertNull(request.getNames());
        assertNull(request.getTimestampStart());
        assertNull(request.getTimestampEnd());
        assertNull(request.getTimestampEnd());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testFindOneWithEmptyResponse() throws Exception {
        final String guid = UUID.randomUUID().toString();
        final long id = System.currentTimeMillis();

        // return empty response for any request
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationSearchResponse(Collections.emptyList()))
                .buildSuccess());

        // call service method
        notificationService.findOne(id, guid)
                .thenAccept(opt -> assertFalse(opt.isPresent()))
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
        assertEquals(Action.NOTIFICATION_SEARCH_REQUEST.name(), argument.getValue().getBody().getAction());

        NotificationSearchRequest request = argument.getValue().getBody().cast(NotificationSearchRequest.class);
        assertEquals(id, request.getId().longValue());
        assertEquals(guid, request.getGuid());
        assertNull(request.getStatus());
        assertNull(request.getNames());
        assertNull(request.getTimestampStart());
        assertNull(request.getTimestampEnd());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testFindOneWithErrorResponse() throws Exception {
        final String guid = UUID.randomUUID().toString();
        final long id = System.currentTimeMillis();
        final String expectedErrorMessage = "EXPECTED ERROR MESSAGE";
        final int errorCode = 500;

        // return fail response for any request
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new ErrorResponse(expectedErrorMessage))
                .buildFailed(errorCode));

        // call service method
        notificationService.findOne(id, guid)
                .thenAccept(opt -> fail("Must be completed exceptionally"))
                .exceptionally(ex -> {
                    assertEquals(BackendException.class, ex.getCause().getClass());
                    assertEquals(expectedErrorMessage, ex.getCause().getMessage());
                    assertEquals(errorCode, ((BackendException) ex.getCause()).getErrorCode());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
        assertEquals(Action.NOTIFICATION_SEARCH_REQUEST.name(), argument.getValue().getBody().getAction());

        NotificationSearchRequest request = argument.getValue().getBody().cast(NotificationSearchRequest.class);
        assertEquals(id, request.getId().longValue());
        assertEquals(guid, request.getGuid());
        assertNull(request.getStatus());
        assertNull(request.getNames());
        assertNull(request.getTimestampStart());
        assertNull(request.getTimestampEnd());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testFindWithEmptyResponse() throws Exception {
        final List<String> guids = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        final Date timestampSt = new Date();
        final Date timestampEnd = new Date();

        final Set<String> guidsForSearch = new HashSet<>(Arrays.asList(
                guids.get(0),
                guids.get(2),
                guids.get(3)));

        // return empty response for any request
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationSearchResponse(Collections.emptyList()))
                .buildSuccess());

        notificationService.find(guidsForSearch, Collections.emptySet(), timestampSt, timestampEnd)
                .thenAccept(notifications -> assertTrue(notifications.isEmpty()))
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(3)).handle(argument.capture());

        NotificationSearchRequest request = argument.getValue().getBody().cast(NotificationSearchRequest.class);
        assertNull(request.getId());
        assertTrue(guidsForSearch.contains(request.getGuid()));
        assertTrue(request.getNames().isEmpty());
        assertEquals(timestampSt, request.getTimestampStart());
        assertEquals(timestampEnd, request.getTimestampEnd());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testFindWithResponse() throws Exception {
        final List<String> guids = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        final Date timestampSt = new Date();
        final Date timestampEnd = new Date();
        final String parameters = "{\"param1\":\"value1\",\"param2\":\"value2\"}";

        final Set<String> guidsForSearch = new HashSet<>(Arrays.asList(
                guids.get(0),
                guids.get(2),
                guids.get(3)));

        // return response for any request
        Map<String, DeviceNotification> notificationMap = guidsForSearch.stream()
                .collect(Collectors.toMap(Function.identity(), guid -> {
                    DeviceNotification notification = new DeviceNotification();
                    notification.setId(System.nanoTime());
                    notification.setDeviceGuid(guid);
                    notification.setNotification(RandomStringUtils.randomAlphabetic(10));
                    notification.setTimestamp(new Date());
                    notification.setParameters(new JsonStringWrapper(parameters));
                    return notification;
                }));

        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            String guid = request.getBody().cast(NotificationSearchRequest.class).getGuid();
            return Response.newBuilder()
                    .withBody(new NotificationSearchResponse(Collections.singletonList(notificationMap.get(guid))))
                    .buildSuccess();
        });


        notificationService.find(guidsForSearch, Collections.emptySet(), timestampSt, timestampEnd)
                .thenAccept(notifications -> {
                    assertEquals(3, notifications.size());
                    assertEquals(new HashSet<>(notificationMap.values()), new HashSet<>(notifications)); // using HashSet to ignore order
                })
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(30, TimeUnit.SECONDS);

        verify(requestHandler, times(3)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testSubmitDeviceNotificationShouldInsertSingleNotification() throws Exception {
        final DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(System.nanoTime());
        deviceVO.setGuid(UUID.randomUUID().toString());

        final DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(System.nanoTime());
        deviceNotification.setTimestamp(new Date());
        deviceNotification.setNotification(RandomStringUtils.randomAlphabetic(10));
        deviceNotification.setDeviceGuid(deviceVO.getGuid());

        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationInsertResponse(deviceNotification))
                .buildSuccess());

        notificationService.insert(deviceNotification, deviceVO)
                .thenAccept(notification -> assertEquals(deviceNotification, notification))
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());

        NotificationInsertRequest request = argument.getValue().getBody().cast(NotificationInsertRequest.class);
        assertEquals(Action.NOTIFICATION_INSERT_REQUEST.name(), request.getAction());
        assertEquals(deviceNotification, request.getDeviceNotification());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testSubmitDeviceNotificationWithRefreshEquipmentShouldInsertSingleNotification() throws Exception {
        // mock DeviceDao
        final DeviceEquipmentService equipmentServiceMock = Mockito.mock(DeviceEquipmentService.class);
        Whitebox.setInternalState(notificationService, "deviceEquipmentService", equipmentServiceMock);

        // create inputs
        final DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(System.nanoTime());
        deviceVO.setGuid(UUID.randomUUID().toString());

        final DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(System.nanoTime());
        deviceNotification.setTimestamp(new Date());
        deviceNotification.setNotification(SpecialNotifications.EQUIPMENT);
        deviceNotification.setDeviceGuid(deviceVO.getGuid());

        // define returns
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withBody(new NotificationInsertResponse(deviceNotification))
                .buildSuccess());

        // execute
        notificationService.insert(deviceNotification, deviceVO)
                .thenAccept(notification -> assertEquals(deviceNotification, notification))
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        // check
        verify(requestHandler, times(1)).handle(argument.capture());
        verify(equipmentServiceMock, times(1)).refreshDeviceEquipment(eq(deviceNotification), eq(deviceVO));

        NotificationInsertRequest request = argument.getValue().getBody().cast(NotificationInsertRequest.class);
        assertEquals(Action.NOTIFICATION_INSERT_REQUEST.name(), request.getAction());
        assertEquals(deviceNotification, request.getDeviceNotification());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testSubmitDeviceNotificationWithRefreshDeviceStatusShouldInsertTwoNotifications() throws Exception {
        // mock DeviceDao
        final DeviceDao deviceDaoMock = Mockito.mock(DeviceDao.class);
        Whitebox.setInternalState(notificationService, "deviceDao", deviceDaoMock);

        // create inputs
        final DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(System.nanoTime());
        deviceVO.setGuid(UUID.randomUUID().toString());

        final DeviceNotification originalNotification = new DeviceNotification();
        originalNotification.setId(System.nanoTime());
        originalNotification.setTimestamp(new Date());
        originalNotification.setNotification(SpecialNotifications.DEVICE_STATUS);
        originalNotification.setDeviceGuid(deviceVO.getGuid());
        originalNotification.setParameters(new JsonStringWrapper("{\"" + Constants.STATUS + "\":\"status1\"}"));

        // define return values
        when(deviceDaoMock.findByUUID(deviceVO.getGuid())).thenReturn(deviceVO);
        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            NotificationInsertRequest insertRequest = invocation.getArgumentAt(0, Request.class)
                    .getBody().cast(NotificationInsertRequest.class);
            return Response.newBuilder()
                    .withBody(new NotificationInsertResponse(insertRequest.getDeviceNotification()))
                    .buildSuccess();
        });

        // execute
        notificationService.insert(originalNotification, deviceVO)
                .thenAccept(resultNotification -> assertEquals(originalNotification, resultNotification))
                .exceptionally(ex -> {
                    fail(ex.toString());
                    return null;
                }).get(15, TimeUnit.SECONDS);

        // check
        verify(requestHandler, times(2)).handle(argument.capture());

        Optional<DeviceNotification> statusNotification = argument.getAllValues().stream()
                .map(r -> r.getBody().cast(NotificationInsertRequest.class).getDeviceNotification())
                .filter(n -> SpecialNotifications.DEVICE_STATUS.equals(n.getNotification())).findFirst();
        assertTrue(statusNotification.isPresent());
        assertEquals(originalNotification, statusNotification.get());

        Optional<DeviceNotification> updateNotification = argument.getAllValues().stream()
                .map(r -> r.getBody().cast(NotificationInsertRequest.class).getDeviceNotification())
                .filter(n -> SpecialNotifications.DEVICE_UPDATE.equals(n.getNotification())).findFirst();
        assertTrue(updateNotification.isPresent());
        assertNotNull(originalNotification.getId());
        assertEquals(deviceVO.getGuid(), updateNotification.get().getDeviceGuid());
    }
}
