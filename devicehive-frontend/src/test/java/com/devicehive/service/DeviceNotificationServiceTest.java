package com.devicehive.service;

import com.devicehive.AbstractFrontendSpringTest;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.ErrorResponse;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.service.exception.BackendException;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceNotificationServiceTest extends AbstractFrontendSpringTest {

    @Autowired
    private DeviceNotificationService notificationService;

    @Test
    @SuppressWarnings("unchecked")
    public void testFindWithErrorResponse() {
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
                }).join();

        verify(requestHandler).handle(argument.capture());
        assertEquals(Action.NOTIFICATION_SEARCH_REQUEST.name(), argument.getValue().getBody().getAction());
        assertEquals(id, argument.getValue().getBody().cast(NotificationSearchRequest.class).getId().longValue());
        assertEquals(guid, argument.getValue().getBody().cast(NotificationSearchRequest.class).getGuid());
    }
}
