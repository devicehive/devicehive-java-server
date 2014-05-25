package com.devicehive.client.impl.context;


import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.context.*;
import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class RestAgent extends AbstractHiveAgent {

    private static final int TIMEOUT = 60;
    private static final String SUB_ID = UUID.randomUUID().toString();

    protected final URI restUri;
    protected final HiveConnectionEventHandler connectionEventHandler;
    private final ExecutorService subscriptionExecutor = Executors.newCachedThreadPool();

    private ConcurrentMap<String, Future> commandSubscriptionsResults = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Future> notificationSubscriptionResults = new ConcurrentHashMap<>();

    private HiveRestConnector restConnector;


    public RestAgent(URI restUri, HiveConnectionEventHandler connectionEventHandler) {
        this.restUri = restUri;
        this.connectionEventHandler = connectionEventHandler;
    }

    public synchronized HiveRestConnector getRestConnector() {
        return restConnector;
    }

    @Override
    public synchronized void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        super.authenticate(hivePrincipal);
        if (restConnector != null) {
            restConnector.setHivePrincipal(hivePrincipal);
        }
    }

    @Override
    protected void beforeConnect() throws HiveException {

    }

    @Override
    protected void doConnect() throws HiveException {
        this.restConnector = new HiveRestConnector(restUri, connectionEventHandler);
        if (!restConnector.isConnected()) {
            restConnector.close();
            throw new HiveException(Messages.INCORRECT_SERVER_URL);
        }
    }

    @Override
    protected void afterConnect() throws HiveException {
        restConnector.setHivePrincipal(getHivePrincipal());
    }

    @Override
    protected void beforeDisconnect() throws HiveException {

    }

    @Override
    protected void doDisconnect() {
        this.restConnector.close();
    }

    @Override
    protected void afterDisonnect() throws HiveException {

    }

    @Override
    public synchronized void close() throws HiveException {
        subscriptionExecutor.shutdownNow();
        super.close();
    }

    public synchronized String addCommandsSubscription(final SubscriptionFilter newFilter,
                                                       final HiveMessageHandler<DeviceCommand> handler)
            throws HiveException {
        final String subscriptionIdValue;
        if (getHivePrincipal().getDevice() != null) {
            subscriptionIdValue = SUB_ID;
        } else {
            subscriptionIdValue = UUID.randomUUID().toString();
        }
        addCommandsSubscription(subscriptionIdValue, new SubscriptionDescriptor<DeviceCommand>(handler, newFilter));

        RestSubscription sub = new RestSubscription() {

            @Override
            protected void execute() throws HiveException {
                Map<String, Object> params = new HashMap<>();
                params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                params.put(Constants.NAMES, newFilter.getNames());
                params.put(Constants.DEVICE_GUIDS, newFilter.getUuids());
                Type responseType = new TypeToken<List<CommandPollManyResponse>>(){}.getType();
                List<CommandPollManyResponse> responses =
                        getRestConnector().executeWithConnectionCheck("/device/command/poll", HttpMethod.GET, null,
                                params, responseType, JsonPolicyDef.Policy.COMMAND_LISTED);
                for (CommandPollManyResponse response : responses) {
                    SubscriptionDescriptor<DeviceCommand> descriptor = getCommandsHandler(subscriptionIdValue);
                    descriptor.updateTimestamp(response.getCommand().getTimestamp());
                    descriptor.getHandler().handle(response.getCommand());
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
    public synchronized void removeCommandsSubscription(String subscriptionId) throws HiveException {
        Future commandsSubscription = commandSubscriptionsResults.remove(subscriptionId);
        if (commandsSubscription != null) {
            commandsSubscription.cancel(true);
        }
        super.removeCommandsSubscription(subscriptionId);
    }

    public synchronized void removeCommandsSubscription() throws HiveException {
        removeCommandsSubscription(SUB_ID);
    }

    public synchronized String addNotificationsSubscription(final SubscriptionFilter newFilter,
                                                            final HiveMessageHandler<DeviceNotification> handler) throws
            HiveException {
        final String subscriptionIdValue = UUID.randomUUID().toString();

        addNotificationsSubscription(subscriptionIdValue, new SubscriptionDescriptor<DeviceNotification>(handler, newFilter));

        RestSubscription sub = new RestSubscription() {


            @Override
            protected void execute() throws HiveException {
                Map<String, Object> params = new HashMap<>();
                params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                params.put(Constants.NAMES, newFilter.getNames());
                params.put(Constants.DEVICE_GUIDS, newFilter.getUuids());
                Type responseType = new TypeToken<List<NotificationPollManyResponse>>(){}.getType();
                List<NotificationPollManyResponse> responses = getRestConnector().executeWithConnectionCheck(
                        "/device/notification/poll",
                        HttpMethod.GET,
                        null,
                        params,
                        responseType,
                        JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
                for (NotificationPollManyResponse response : responses) {
                    SubscriptionDescriptor<DeviceNotification> descriptor = getNotificationsHandler(subscriptionIdValue);
                    descriptor.updateTimestamp(response.getNotification().getTimestamp());
                    descriptor.getHandler().handle(response.getNotification());
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
        return getRestConnector().executeWithConnectionCheck("/info", HttpMethod.GET, null, ApiInfo.class, null);
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
