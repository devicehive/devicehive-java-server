package com.devicehive.client.impl.context;


import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.reflect.TypeToken;

import com.devicehive.client.ConnectionLostCallback;
import com.devicehive.client.ConnectionRestoredCallback;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.impl.json.strategies.JsonPolicyApply;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.impl.rest.providers.CollectionProvider;
import com.devicehive.client.impl.rest.providers.HiveEntityProvider;
import com.devicehive.client.impl.rest.providers.JsonRawProvider;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.CommandPollManyResponse;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.ErrorMessage;
import com.devicehive.client.model.NotificationPollManyResponse;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;

public class RestAgent extends AbstractHiveAgent {

    private static final String USER_AUTH_SCHEMA = "Basic";
    private static final String KEY_AUTH_SCHEMA = "Bearer";
    private static final int TIMEOUT = 60;
    protected final URI restUri;
    protected final ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(50);
    private ConcurrentMap<String, Future<?>> commandSubscriptionsResults = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Future<?>> notificationSubscriptionResults = new ConcurrentHashMap<>();
    private Client restClient;

    public RestAgent(ConnectionLostCallback connectionLostCallback,
                     ConnectionRestoredCallback connectionRestoredCallback, URI restUri) {
        super(connectionLostCallback, connectionRestoredCallback);
        this.restUri = restUri;
    }

    @Override
    protected void beforeConnect() throws HiveException {

    }

    @Override
    protected void doConnect() throws HiveException {
        this.restClient = JerseyClientBuilder.createClient();
        this.restClient.register(JsonRawProvider.class)
            .register(HiveEntityProvider.class)
            .register(CollectionProvider.class);
    }

    @Override
    protected void afterConnect() throws HiveException {

    }

    @Override
    protected void beforeDisconnect() {
        MoreExecutors.shutdownAndAwaitTermination(subscriptionExecutor, 1, TimeUnit.MINUTES);
        commandSubscriptionsResults.clear();
        notificationSubscriptionResults.clear();
    }

    @Override
    protected void doDisconnect() {
        this.restClient.close();
    }

    @Override
    protected void afterDisconnect() {
    }

    public boolean checkConnection() {
        try {
            Response response = buildInvocation("/info", HttpMethod.GET, null, null, null, null).invoke();
            getEntity(response, ApiInfo.class, null);
            return true;
        } catch (HiveException e) {
            return false;
        }
    }

    /**
     * Executes request with following params
     *
     * @param path         requested uri
     * @param method       http method
     * @param headers      custom headers (authorization headers are added during the request build)
     * @param objectToSend Object to send (for http methods POST and PUT only)
     * @param sendPolicy   policy that declares exclusion strategy for sending object
     */
    public <S> void execute(String path, String method, Map<String, String> headers, S objectToSend,
                            JsonPolicyDef.Policy sendPolicy) throws HiveException {
        execute(path, method, headers, null, objectToSend, null, sendPolicy, null);
    }

    /**
     * Executes request with following params
     *
     * @param path        requested uri
     * @param method      http method
     * @param headers     custom headers (authorization headers are added during the request build)
     * @param queryParams query params that should be added to the url. Null-valued params are ignored.
     */
    public void execute(String path, String method, Map<String, String> headers,
                        Map<String, Object> queryParams)
        throws HiveException {
        execute(path, method, headers, queryParams, null, null, null, null);
    }

    /**
     * Executes request with following params
     *
     * @param path   requested uri
     * @param method http method
     */
    public void execute(String path, String method) throws HiveException {
        execute(path, method, null, null, null, null, null, null);
    }

    /**
     * Executes request with following params
     *
     * @param path          requested uri
     * @param method        http method
     * @param headers       custom headers (authorization headers are added during the request build)
     * @param queryParams   query params that should be added to the url. Null-valued params are ignored.
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such
     *                      classes
     * @param receivePolicy policy that declares exclusion strategy for received object
     * @return instance of typeOfR, that represents server's response
     */
    public <R> R execute(String path, String method, Map<String, String> headers,
                         Map<String, Object> queryParams,
                         Type typeOfR, JsonPolicyDef.Policy receivePolicy) throws HiveException {
        return execute(path, method, headers, queryParams, null, typeOfR, null, receivePolicy);
    }

    /**
     * Executes request with following params
     *
     * @param path          requested uri
     * @param method        http method
     * @param headers       custom headers (authorization headers are added during the request build)
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such
     *                      classes
     * @param receivePolicy policy that declares exclusion strategy for received object
     * @return instance of typeOfR, that represents server's response
     */
    public <R> R execute(String path, String method, Map<String, String> headers, Type typeOfR,
                         JsonPolicyDef.Policy receivePolicy) throws HiveException {
        return execute(path, method, headers, null, null, typeOfR, null, receivePolicy);
    }

    /**
     * Executes request with following params using forms
     *
     * @param path          requested uri
     * @param formParams    form parameters
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such
     *                      classes
     * @param receivePolicy policy that declares exclusion strategy for received object
     * @return instance of TypeOfR or null
     */
    public synchronized <R> R executeForm(String path, Map<String, String> formParams, Type typeOfR,
                                          JsonPolicyDef.Policy receivePolicy) throws HiveException {
        connectionLock.readLock().lock();
        try {
            Response response = buildFormInvocation(path, formParams).invoke();
            return getEntity(response, typeOfR, receivePolicy);
        } catch (ProcessingException e) {
            throw new HiveException(Messages.INVOKE_TARGET_ERROR, e.getCause());
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    //Private methods------------------------------------------------------------------------------------------

    /**
     * Executes request with following params
     *
     * @param path          requested uri
     * @param method        http method
     * @param headers       custom headers (authorization headers are added during the request build)
     * @param queryParams   query params that should be added to the url. Null-valued params are ignored.
     * @param objectToSend  Object to send (for http methods POST and PUT only)
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such
     *                      classes
     * @param sendPolicy    policy that declares exclusion strategy for sending object
     * @param receivePolicy policy that declares exclusion strategy for received object
     * @return instance of TypeOfR or null
     */
    public <S, R> R execute(String path, String method, Map<String, String> headers,
                            Map<String, Object> queryParams,
                            S objectToSend, Type typeOfR, JsonPolicyDef.Policy sendPolicy,
                            JsonPolicyDef.Policy receivePolicy) throws HiveException {
        connectionLock.readLock().lock();
        try {
            Response response = buildInvocation(path, method, headers, queryParams, objectToSend, sendPolicy).invoke();
            return getEntity(response, typeOfR, receivePolicy);
        } catch (ProcessingException e) {
            throw new HiveException(Messages.INVOKE_TARGET_ERROR, e.getCause());
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    private <R> R getEntity(Response response, Type typeOfR, JsonPolicyDef.Policy receivePolicy) throws HiveException {
        Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
        switch (statusFamily) {
            case SERVER_ERROR:
                throw new HiveServerException(response.getStatus());
            case CLIENT_ERROR:
                if (response.getStatus() == METHOD_NOT_ALLOWED.getStatusCode()) {
                    throw new InternalHiveClientException(METHOD_NOT_ALLOWED.getReasonPhrase(),
                                                          response.getStatus());
                }
                ErrorMessage errorMessage = response.readEntity(ErrorMessage.class);
                throw new HiveClientException(errorMessage.getMessage(), response.getStatus());
            case SUCCESSFUL:
                if (typeOfR == null) {
                    return null;
                }
                if (receivePolicy == null) {
                    return response.readEntity(new GenericType<R>(typeOfR));
                } else {
                    Annotation[] readAnnotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(receivePolicy)};
                    return response.readEntity(new GenericType<R>(typeOfR), readAnnotations);
                }
            default:
                throw new HiveException(Messages.UNKNOWN_RESPONSE);
        }
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = Maps.newHashMap();
        HivePrincipal hivePrincipal = getHivePrincipal();
        if (getHivePrincipal() != null) {
            if (hivePrincipal.getUser() != null) {
                String decodedAuth = hivePrincipal.getUser().getLeft() + ":" + hivePrincipal.getUser().getRight();
                String encodedAuth = Base64.encodeBase64String(decodedAuth.getBytes(Constants.UTF8_CHARSET));
                headers.put(HttpHeaders.AUTHORIZATION, USER_AUTH_SCHEMA + " " + encodedAuth);
            }
            if (hivePrincipal.getDevice() != null) {
                headers.put(Constants.DEVICE_ID_HEADER, hivePrincipal.getDevice().getLeft());
                headers.put(Constants.DEVICE_KEY_HEADER, hivePrincipal.getDevice().getRight());
            }
            if (hivePrincipal.getAccessKey() != null) {
                headers.put(HttpHeaders.AUTHORIZATION, KEY_AUTH_SCHEMA + " " + hivePrincipal.getAccessKey());
            }
        }
        return headers;
    }

    private WebTarget createTarget(String path, Map<String, Object> queryParams) {
        WebTarget target = restClient.target(restUri).path(path);
        if (queryParams != null) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                Object value = entry.getValue() instanceof Timestamp
                               ? TimestampAdapter.formatTimestamp((Timestamp) entry.getValue())
                               : entry.getValue();
                target = target.queryParam(entry.getKey(), value);
            }
        }
        return target;
    }

    private <S> Invocation buildInvocation(String path,
                                           String method,
                                           Map<String, String> headers,
                                           Map<String, Object> queryParams,
                                           S objectToSend,
                                           JsonPolicyDef.Policy sendPolicy) {
        Invocation.Builder invocationBuilder = createTarget(path, queryParams)
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE);
        for (Map.Entry<String, String> entry : getAuthHeaders().entrySet()) {
            invocationBuilder.header(entry.getKey(), entry.getValue());
        }
        if (headers != null) {
            for (Map.Entry<String, String> customHeader : headers.entrySet()) {
                invocationBuilder.header(customHeader.getKey(), customHeader.getValue());
            }
        }
        if (objectToSend != null) {
            Entity<S> entity;
            if (sendPolicy != null) {
                entity = Entity.entity(objectToSend, MediaType.APPLICATION_JSON_TYPE,
                                       new Annotation[]{new JsonPolicyApply.JsonPolicyApplyLiteral(sendPolicy)});
            } else {
                entity = Entity.entity(objectToSend, MediaType.APPLICATION_JSON_TYPE);
            }
            return invocationBuilder.build(method, entity);
        } else {
            return invocationBuilder.build(method);
        }
    }

    private WebTarget createTarget(String path) {
        return createTarget(path, null);
    }

    private Invocation buildFormInvocation(String path, Map<String, String> formParams) throws HiveException {
        Invocation.Builder invocationBuilder = createTarget(path).
            request().
            accept(MediaType.APPLICATION_JSON_TYPE).
            header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        for (Map.Entry<String, String> entry : getAuthHeaders().entrySet()) {
            invocationBuilder.header(entry.getKey(), entry.getValue());
        }

        Entity<Form> entity;
        if (formParams != null) {
            Form f = new Form();
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                f.param(entry.getKey(), entry.getValue());
            }
            entity = Entity.entity(f, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
            return invocationBuilder.build(HttpMethod.POST, entity);
        }
        throw new InternalHiveClientException(Messages.FORM_PARAMS_ARE_NULL);
    }

    public String subscribeForCommands(final SubscriptionFilter newFilter,
                                       final HiveMessageHandler<DeviceCommand> handler)
        throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String subscriptionIdValue = UUID.randomUUID().toString();
            commandSubscriptionsStorage.put(subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));

            RestSubscription sub = new RestSubscription() {

                @Override
                protected void execute() throws HiveException {
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    if (newFilter != null) {
                        params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                        params.put(Constants.NAMES, StringUtils.join(newFilter.getNames(), Constants.SEPARATOR));
                        params.put(Constants.DEVICE_GUIDS, StringUtils.join(newFilter.getUuids(), Constants.SEPARATOR));
                    }
                    Type responseType = new TypeToken<List<CommandPollManyResponse>>() {
                    }.getType();
                    while (!Thread.currentThread().isInterrupted()) {
                        List<CommandPollManyResponse> responses =
                            RestAgent.this.execute("/device/command/poll", HttpMethod.GET, null,
                                                   params, responseType, JsonPolicyDef.Policy.COMMAND_LISTED);
                        for (CommandPollManyResponse response : responses) {
                            SubscriptionDescriptor<DeviceCommand> descriptor =
                                commandSubscriptionsStorage.get(subscriptionIdValue);
                            descriptor.handleMessage(response.getCommand());
                        }
                        if (!responses.isEmpty()) {
                            Timestamp newTimestamp = responses.get(responses.size() - 1).getCommand().getTimestamp();
                            params.put(Constants.TIMESTAMP, newTimestamp);
                        }
                    }
                }
            };

            Future<?> commandsSubscription = subscriptionExecutor.submit(sub);
            commandSubscriptionsResults.put(subscriptionIdValue, commandsSubscription);
            return subscriptionIdValue;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    public String subscribeForCommandsForDevice(final SubscriptionFilter newFilter,
                                                final HiveMessageHandler<DeviceCommand> handler)
        throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String subscriptionIdValue = UUID.randomUUID().toString();
            commandSubscriptionsStorage.put(subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));

            RestSubscription sub = new RestSubscription() {

                @Override
                protected void execute() throws HiveException {
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    if (newFilter != null) {
                        params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                        params.put(Constants.NAMES, StringUtils.join(newFilter.getNames(), Constants.SEPARATOR));
                    }
                    Type responseType = new TypeToken<List<DeviceCommand>>() {
                    }.getType();
                    String uri = String.format("/device/%s/command/poll", getHivePrincipal().getDevice().getLeft());
                    while (!Thread.currentThread().isInterrupted()) {
                        List<DeviceCommand> responses =
                            RestAgent.this.execute(uri,
                                                   HttpMethod.GET,
                                                   null,
                                                   params, responseType, JsonPolicyDef.Policy.COMMAND_LISTED);
                        for (DeviceCommand response : responses) {
                            SubscriptionDescriptor<DeviceCommand> descriptor =
                                commandSubscriptionsStorage.get(subscriptionIdValue);
                            descriptor.handleMessage(response);
                        }
                        if (!responses.isEmpty()) {
                            Timestamp newTimestamp = responses.get(responses.size() - 1).getTimestamp();
                            params.put(Constants.TIMESTAMP, newTimestamp);
                        }
                    }
                }
            };

            Future<?> commandsSubscription = subscriptionExecutor.submit(sub);
            commandSubscriptionsResults.put(subscriptionIdValue, commandsSubscription);
            return subscriptionIdValue;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    /**
     * Remove command subscription.
     */
    public void unsubscribeFromCommands(String subscriptionId) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            Future<?> commandsSubscription = commandSubscriptionsResults.remove(subscriptionId);
            if (commandsSubscription != null) {
                commandsSubscription.cancel(true);
            }
            commandSubscriptionsStorage.remove(subscriptionId);
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    public void subscribeForCommandUpdates(final Long commandId,
                                           final String guid,
                                           final HiveMessageHandler<DeviceCommand> handler)
        throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            RestSubscription sub = new RestSubscription() {
                @Override
                protected void execute() throws HiveException {
                    DeviceCommand result = null;
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    Type responseType = new TypeToken<DeviceCommand>() {
                    }.getType();
                    while (result == null && !Thread.currentThread().isInterrupted()) {
                        result = RestAgent.this.execute(
                            String.format("/device/%s/command/%s/poll", guid, commandId),
                            HttpMethod.GET,
                            null,
                            params,
                            responseType,
                            JsonPolicyDef.Policy.COMMAND_TO_DEVICE);
                    }
                    handler.handle(result);
                }
            };
            subscriptionExecutor.submit(sub);
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    public String subscribeForNotifications(final SubscriptionFilter newFilter,
                                            final HiveMessageHandler<DeviceNotification> handler) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String subscriptionIdValue = UUID.randomUUID().toString();
            notificationSubscriptionsStorage.put(subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));
            RestSubscription sub = new RestSubscription() {
                @Override
                protected void execute() throws HiveException {
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    if (newFilter != null) {
                        params.put(Constants.TIMESTAMP, newFilter.getTimestamp());
                        params.put(Constants.NAMES, StringUtils.join(newFilter.getNames(), Constants.SEPARATOR));
                        params.put(Constants.DEVICE_GUIDS, StringUtils.join(newFilter.getUuids(), Constants.SEPARATOR));
                    }
                    Type responseType = new TypeToken<List<NotificationPollManyResponse>>() {
                    }.getType();
                    while (!Thread.currentThread().isInterrupted()) {
                        List<NotificationPollManyResponse> responses = RestAgent.this.execute(
                            "/device/notification/poll",
                            HttpMethod.GET,
                            null,
                            params,
                            responseType,
                            JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
                        for (NotificationPollManyResponse response : responses) {
                            SubscriptionDescriptor<DeviceNotification>
                                descriptor =
                                notificationSubscriptionsStorage.get(
                                    subscriptionIdValue);
                            descriptor.handleMessage(response.getNotification());
                        }
                        if (!responses.isEmpty()) {
                            Timestamp newTimestamp = responses.get(responses.size() - 1).getNotification().getTimestamp();
                            params.put(Constants.TIMESTAMP, newTimestamp);
                        }
                    }
                }
            };
            Future<?> notificationsSubscription = subscriptionExecutor.submit(sub);
            notificationSubscriptionResults.put(subscriptionIdValue, notificationsSubscription);
            return subscriptionIdValue;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    /**
     * Remove command subscription for all available commands.
     */
    public void unsubscribeFromNotifications(String subscriptionId) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            Future<?> notificationsSubscription = notificationSubscriptionResults.remove(subscriptionId);
            if (notificationsSubscription != null) {
                notificationsSubscription.cancel(true);
            }
            notificationSubscriptionsStorage.remove(subscriptionId);
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    /**
     * Get API info from server
     *
     * @return API info
     */
    public ApiInfo getInfo() throws HiveException {
        return execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
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
            try {
                execute();
            } catch (Throwable e) {
                logger.error(Messages.SUBSCRIPTION_ERROR, e);
            }
        }
    }
}
