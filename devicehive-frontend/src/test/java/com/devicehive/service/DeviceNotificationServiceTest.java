package com.devicehive.service;

import com.devicehive.base.AbstractSpringKafkaTest;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.ErrorResponse;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.service.exception.BackendException;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @SuppressWarnings("unchecked")
    public void testFindWithErrorResponse() throws InterruptedException, ExecutionException, TimeoutException {
        final String guid = UUID.randomUUID().toString();
        final long id = System.currentTimeMillis();
        final String expectedErrorMessage = "EXPECTED ERROR MESSAGE";
        final int errorCode = 500;

        // return fail response for any request
        when(requestHandler.handle(any(Request.class))).thenReturn(Response.newBuilder()
                .withErrorCode(500)
                .withBody(new ErrorResponse(expectedErrorMessage))
                .buildFailed());

        // call service method
        notificationService.find(id, guid)
                .thenAccept(opt -> fail("Must be completed exceptionally"))
                .exceptionally(ex -> {
                    assertEquals(BackendException.class, ex.getCause().getClass());
                    assertEquals(expectedErrorMessage, ex.getCause().getMessage());
                    assertEquals(errorCode, ((BackendException) ex.getCause()).getErrorCode());
                    return null;
                }).get(2, TimeUnit.SECONDS); // TODO: use org.junit.Test.timeout()

        verify(requestHandler).handle(argument.capture());
        assertEquals(Action.NOTIFICATION_SEARCH_REQUEST.name(), argument.getValue().getBody().getAction());
        assertEquals(id, argument.getValue().getBody().cast(NotificationSearchRequest.class).getId().longValue());
        assertEquals(guid, argument.getValue().getBody().cast(NotificationSearchRequest.class).getGuid());
    }
}
