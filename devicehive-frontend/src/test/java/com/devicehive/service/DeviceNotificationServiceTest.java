package com.devicehive.service;

import com.devicehive.base.AbstractSpringKafkaTest;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.*;
import com.devicehive.service.exception.BackendException;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class DeviceNotificationServiceTest extends AbstractSpringKafkaTest {

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
    public void testFindOneWithResponse() throws InterruptedException, ExecutionException, TimeoutException {
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
                    fail("Must be completed successfully");
                    return null;
                }).get(2, TimeUnit.SECONDS);

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
    public void testFindOneWithEmptyResponse() throws InterruptedException, ExecutionException, TimeoutException {
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
                    fail("Must be completed successfully");
                    return null;
                }).get(2, TimeUnit.SECONDS);

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
    public void testFindOneWithErrorResponse() throws InterruptedException, ExecutionException, TimeoutException {
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
                }).get(2, TimeUnit.SECONDS);

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
    public void testFindWithEmptyResponse() throws InterruptedException, ExecutionException, TimeoutException {
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
                .thenAccept(notifications -> {
                    assertTrue(notifications.isEmpty());
                })
                .exceptionally(ex -> {
                    fail("Must be completed successfully");
                    return null;
                }).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(3)).handle(argument.capture());

        NotificationSearchRequest request = argument.getValue().getBody().cast(NotificationSearchRequest.class);
        assertNull(request.getId());
        assertTrue(guidsForSearch.contains(request.getGuid()));
        assertTrue(request.getNames().isEmpty());
        assertEquals(timestampSt, request.getTimestampStart());
        assertEquals(timestampEnd, request.getTimestampEnd());
    }

    @Test
    public void testFindWithResponse() throws InterruptedException, ExecutionException, TimeoutException {
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

        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
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
                    fail("Must be completed successfully");
                    return null;
                }).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(3)).handle(argument.capture());
    }

    @Test
    public void testSubmitDeviceNotificationShouldPass() throws InterruptedException {
        final DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(System.nanoTime());
        deviceVO.setGuid(UUID.randomUUID().toString());

        final DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(System.nanoTime());
        deviceNotification.setTimestamp(new Date());
        deviceNotification.setNotification(RandomStringUtils.randomAlphabetic(10));
        deviceNotification.setDeviceGuid(deviceVO.getGuid());

        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder().buildSuccess());

        notificationService.submitDeviceNotification(deviceNotification, deviceVO);
        TimeUnit.SECONDS.sleep(2);

        verify(requestHandler, times(1)).handle(argument.capture());

        NotificationInsertRequest request = argument.getValue().getBody().cast(NotificationInsertRequest.class);
        assertEquals(Action.NOTIFICATION_INSERT_REQUEST.name(), request.getAction());
        assertEquals(deviceNotification, request.getDeviceNotification());
    }
}
