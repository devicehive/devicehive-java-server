package com.devicehive.messages.handler;

import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.response.CommandPollManyResponse;
import com.devicehive.model.response.NotificationPollManyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.UUID;

public abstract class RestHandlerCreator<T> implements HandlerCreator<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestHandlerCreator.class);

    private final AsyncResponse asyncResponse;

    private RestHandlerCreator(final AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    public static RestHandlerCreator<DeviceNotification> createNotificationInsert(final AsyncResponse asyncResponse, final boolean isMany) {
        return new RestHandlerCreator<DeviceNotification>(asyncResponse) {
            @Override
            protected Response createResponse(DeviceNotification message) {
                LOGGER.debug("NotificationInsert created for message: {}", message);
                final HiveEntity responseMessage = isMany ? new NotificationPollManyResponse(message, message.getDeviceGuid()) : message;
                return ResponseFactory.response(Response.Status.OK, Arrays.asList(responseMessage), JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
            }
        };
    }

    public static RestHandlerCreator<DeviceCommand> createCommandInsert(final AsyncResponse asyncResponse, final boolean isMany) {
        return new RestHandlerCreator<DeviceCommand>(asyncResponse) {
            @Override
            protected Response createResponse(DeviceCommand message) {
                LOGGER.debug("CommandInsert created for message: {}", message);
                final HiveEntity responseMessage = isMany ? new CommandPollManyResponse(message, message.getDeviceGuid()) : message;
                return ResponseFactory.response(Response.Status.OK, Arrays.asList(responseMessage), JsonPolicyDef.Policy.COMMAND_LISTED);
            }
        };
    }

    public static RestHandlerCreator<DeviceCommand> createCommandUpdate(final AsyncResponse asyncResponse) {
        return new RestHandlerCreator<DeviceCommand>(asyncResponse) {
            @Override
            protected Response createResponse(DeviceCommand message) {
                LOGGER.debug("CommandUpdateInsert created for message: {}", message);
                return ResponseFactory.response(Response.Status.OK, message, JsonPolicyDef.Policy.COMMAND_TO_DEVICE);
            }
        };
    }

    protected abstract Response createResponse(T message);

    @Override
    public Runnable getHandler(final T message, final UUID subId) {
        LOGGER.debug("Rest subscription notified");

        return new Runnable() {
            @Override
            public void run() {
                Response response = createResponse(message);
                asyncResponse.resume(response);
            }
        };
    }
}
