package com.devicehive.client.impl.context;

import com.devicehive.client.MessageHandler;
import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.impl.util.HiveTypeToken;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.CommandPollManyResponse;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.NotificationPollManyResponse;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


public class HiveRestContext extends AbstractHiveContext {

    private static final int TIMEOUT = 60;
    private final ExecutorService subscriptionExecutor = Executors.newCachedThreadPool();
    private final AtomicInteger subscriptionId = new AtomicInteger(0);
    private HiveRestConnector restConnector;
    private Map<String, Future> commandSubscriptionsResults = new HashMap<>();
    private Map<String, Future> notificationSubscriptionResults = new HashMap<>();

    /**
     * @param commandUpdatesHandler
     */
    public HiveRestContext(URI restUri,
                           MessageHandler<DeviceCommand> commandUpdatesHandler,
                           HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        super(commandUpdatesHandler);
        this.restConnector = new HiveRestConnector(restUri, this, connectionEventHandler);
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

    public synchronized String addCommandsSubscription(final SubscriptionFilter newFilter,
                                                       final MessageHandler<DeviceCommand> handler) throws
            HiveException {
        final String subscriptionIdValue = String.valueOf(subscriptionId.incrementAndGet());
        addCommandsSubscription(subscriptionIdValue, handler);

        RestSubscription sub = new RestSubscription() {
            private final SubscriptionFilter filter = newFilter;

            @Override
            protected void execute() throws HiveException {
                Map<String, Object> params = new HashMap<>();
                params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                params.put(Constants.NAMES, newFilter.getNames());
                params.put(Constants.DEVICE_GUIDS, newFilter.getUuids());
                Type responseType = new HiveTypeToken<List<CommandPollManyResponse>>().getType();
                List<CommandPollManyResponse> responses =
                        restConnector.execute("/device/command/poll", HttpMethod.GET, null,
                                params, responseType, JsonPolicyDef.Policy.COMMAND_LISTED);
                for (CommandPollManyResponse response : responses) {
                    Timestamp timestamp = filter.getTimestamp();
                    if (timestamp == null || timestamp.before(response.getCommand().getTimestamp())) {
                        filter.setTimestamp(response.getCommand().getTimestamp());
                    }
                    getCommandsHandler(subscriptionIdValue).handle(response.getCommand());
                }
            }
        };
        Future commandsSubscription = subscriptionExecutor.submit(sub);
        commandSubscriptionsResults.put(subscriptionIdValue, commandsSubscription);
        return subscriptionIdValue;
    }

    /**
     * Remove command subscription.
     */
    @Override
    public synchronized void removeCommandsSubscription(String subscriptionId) throws HiveException {
        Future commandsSubscription = commandSubscriptionsResults.remove(subscriptionId);
        if (commandsSubscription != null) {
            commandsSubscription.cancel(true);
        }
        super.removeCommandsSubscription(subscriptionId);
    }

    public synchronized String addNotificationsSubscription(final SubscriptionFilter newFilter,
                                                            final MessageHandler<DeviceNotification> handler) throws
            HiveException {
        final String subscriptionIdValue = String.valueOf(subscriptionId.incrementAndGet());
        addNotificationsSubscription(subscriptionIdValue, handler);
        RestSubscription sub = new RestSubscription() {

            private final SubscriptionFilter filter = newFilter;

            @Override
            protected void execute() throws HiveException {
                Map<String, Object> params = new HashMap<>();
                params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                params.put(Constants.NAMES, newFilter.getNames());
                params.put(Constants.DEVICE_GUIDS, newFilter.getUuids());
                Type responseType = new HiveTypeToken<List<NotificationPollManyResponse>>().getType();
                List<NotificationPollManyResponse> responses = restConnector.execute(
                        "/device/command/poll",
                        HttpMethod.GET,
                        null,
                        params,
                        responseType,
                        JsonPolicyDef.Policy.COMMAND_LISTED);
                for (NotificationPollManyResponse response : responses) {
                    Timestamp timestamp = filter.getTimestamp();

                    if (timestamp == null || timestamp.before(response.getNotification().getTimestamp())) {
                        filter.setTimestamp(response.getNotification().getTimestamp());
                    }
                    getNotificationsHandler(subscriptionIdValue).handle(response.getNotification());
                }
            }
        };
        Future notificationsSubscription = subscriptionExecutor.submit(sub);
        notificationSubscriptionResults.put(subscriptionIdValue, notificationsSubscription);
        return subscriptionIdValue;
    }

    /**
     * Remove command subscription for all available commands.
     */
    @Override
    public synchronized void removeNotificationsSubscription(String subscriptionId) throws HiveException {
        Future notificationsSubscription = notificationSubscriptionResults.remove(subscriptionId);
        if (notificationsSubscription != null) {
            notificationsSubscription.cancel(true);
        }
        super.removeNotificationsSubscription(subscriptionId);
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
