package com.devicehive.messages.handler;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.resource.util.ResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.FutureTask;

@Deprecated
public abstract class RestHandlerCreator<T> implements HandlerCreator<T> {
    private static final Logger logger = LoggerFactory.getLogger(RestHandlerCreator.class);

    private final AsyncResponse asyncResponse;

    private RestHandlerCreator(final AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    public static RestHandlerCreator<DeviceNotification> createNotificationInsert(final AsyncResponse asyncResponse, final boolean isMany, final FutureTask<Void> waitTask) {
        return new RestHandlerCreator<DeviceNotification>(asyncResponse) {
            @Override
            protected Response createResponse(DeviceNotification message) {
                logger.debug("NotificationInsert created for message: {}", message);
                waitTask.cancel(false);
                return ResponseFactory.response(Response.Status.OK, Collections.singletonList(message), JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
            }
        };
    }

    public static RestHandlerCreator<DeviceCommand> createCommandInsert(final AsyncResponse asyncResponse, final boolean isMany, final FutureTask<Void> waitTask) {
        return new RestHandlerCreator<DeviceCommand>(asyncResponse) {
            @Override
            protected Response createResponse(DeviceCommand message) {
                logger.debug("CommandInsert created for message: {}", message);
                waitTask.cancel(false);
                return ResponseFactory.response(Response.Status.OK, Collections.singletonList(message), JsonPolicyDef.Policy.COMMAND_LISTED);
            }
        };
    }

    public static RestHandlerCreator<DeviceCommand> createCommandUpdate(final AsyncResponse asyncResponse) {
        return new RestHandlerCreator<DeviceCommand>(asyncResponse) {
            @Override
            protected Response createResponse(DeviceCommand message) {
                logger.debug("CommandUpdateInsert created for message: {}", message);
                return ResponseFactory.response(Response.Status.OK, message, JsonPolicyDef.Policy.COMMAND_TO_DEVICE);
            }
        };
    }

    protected abstract Response createResponse(T message);

    @Override
    public Runnable getHandler(final T message, final UUID subId) {
        logger.debug("Rest subscription notified");

        return new Runnable() {
            @Override
            public void run() {
                Response response = createResponse(message);
                asyncResponse.resume(response);
            }
        };
    }
}
