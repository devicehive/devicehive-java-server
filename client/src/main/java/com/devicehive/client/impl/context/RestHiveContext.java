package com.devicehive.client.impl.context;

import com.devicehive.client.MessageHandler;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.devicehive.client.impl.context.Constants.REQUIRED_VERSION_OF_API;


public class RestHiveContext extends AbstractHiveContext {

    private static final int TIMEOUT = 60;

    private HiveRestConnector restConnector;
    private final ExecutorService subscriptionExecutor = Executors.newCachedThreadPool();
    private Future commandsSubscription;
    private Future notificationsSubscription;

    /**

     *
     * @param commandsHandler       handler for incoming commands and command updates
     * @param commandUpdatesHandler
     * @param notificationsHandler  handler for incoming notifications
     */
    public RestHiveContext(ConnectionDescriptor connectionDescriptor, MessageHandler<DeviceCommand> commandsHandler, MessageHandler<DeviceCommand> commandUpdatesHandler, MessageHandler<DeviceNotification> notificationsHandler) {
        super(commandsHandler, commandUpdatesHandler, notificationsHandler);
        this.restConnector = new HiveRestConnector(connectionDescriptor.getRestURI(), this);
    }


    public HiveRestConnector getRestConnector() {
        return restConnector;
    }

    @Override
    public synchronized void close() {
        super.close();
        subscriptionExecutor.shutdownNow();
        restConnector.close();
    }

    public synchronized void addCommandsSubscription(final SubscriptionFilter newFilter) throws HiveException {
        removeCommandsSubscription();

        RestSubscription sub = new RestSubscription() {

            private final SubscriptionFilter filter = newFilter;

            @Override
            protected void execute() throws HiveException {
                Map<String, String> formParams = new HashMap<>();
                formParams.put(WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                formParams.put(FILTER_PARAM, GsonFactory.createGson().toJson(filter));

                @SuppressWarnings("SerializableHasSerializationMethods")
                List<CommandPollManyResponse> responses = null;
                /*
                            executeForm("/device/command/poll", formParams, new TypeToken<List<CommandPollManyResponse>>() {
                            }.getType(), COMMAND_LISTED);
                */
                for (CommandPollManyResponse response : responses) {
                    Timestamp timestamp = filter.getTimestamp();
                    if (timestamp == null || timestamp.before(response.getCommand().getTimestamp())) {
                        filter.setTimestamp(response.getCommand().getTimestamp());
                    }
                    getCommandsHandler().handle(response.getCommand());
                }
            }
        };
        commandsSubscription = subscriptionExecutor
                .submit(sub);
    }

    /**
     * Remove command subscription for all available commands.
     */
    public synchronized void removeCommandsSubscription() throws HiveException {
        if (commandsSubscription != null) {
            commandsSubscription.cancel(true);
            commandsSubscription = null;
        }
    }


    public synchronized void addNotificationsSubscription(final SubscriptionFilter newFilter) throws HiveException {
        removeNotificationsSubscription();
        RestSubscription sub = new RestSubscription() {

            private final SubscriptionFilter filter = newFilter;

            @Override
            protected void execute() throws HiveException {
                Map<String, String> formParams = new HashMap<>();
                formParams.put(WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                formParams.put(FILTER_PARAM, GsonFactory.createGson().toJson(filter));

                @SuppressWarnings("SerializableHasSerializationMethods")
                List<NotificationPollManyResponse> responses = null;
                /*
                        executeForm("/device/command/poll", formParams, new TypeToken<List<NotificationPollManyResponse>>() {
                        }.getType(), COMMAND_LISTED);
                */
                for (NotificationPollManyResponse response : responses) {
                    Timestamp timestamp = filter.getTimestamp();

                    if (timestamp == null || timestamp.before(response.getNotification().getTimestamp())) {
                        filter.setTimestamp(response.getNotification().getTimestamp());
                    }
                    getNotificationsHandler().handle(response.getNotification());
                }
            }
        };
        notificationsSubscription = subscriptionExecutor
                .submit(sub);
    }

    /**
     * Remove command subscription for all available commands.
     */
    public synchronized void removeNotificationsSubscription() throws HiveException {
        if (notificationsSubscription != null) {
            notificationsSubscription.cancel(true);
            notificationsSubscription = null;
        }
    }



    /**
     * Get API info from server
     *
     * @return API info
     */
    public ApiInfo getInfo() throws HiveException {
        return restConnector.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
    }


    public Timestamp getServerTimestamp() throws HiveException {
        return getInfo().getServerTimestamp();
    }

    public String getServerApiVersion() throws HiveException {
        return getInfo().getApiVersion();
    }

    private abstract static class RestSubscription implements Runnable {

        private static Logger logger = LoggerFactory.getLogger(RestSubscription.class);

        protected static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
        protected static final String FILTER_PARAM = "subscription";

        protected abstract void execute() throws HiveException;

        @Override
        public final void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    execute();
                } catch (Throwable e) {
                    logger.error("Error processing subscription", e);
                }
            }
        }
    }
}
