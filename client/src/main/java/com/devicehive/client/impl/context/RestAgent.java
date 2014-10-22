package com.devicehive.client.impl.context;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.reflect.TypeToken;

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
import java.nio.charset.Charset;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

public class RestAgent {

    private static final String USER_AUTH_SCHEMA = "Basic";
    private static final String KEY_AUTH_SCHEMA = "Bearer";
    private static final int TIMEOUT = 60;
    private static final String DEVICE_ID_HEADER = "Auth-DeviceID";
    private static final String DEVICE_KEY_HEADER = "Auth-DeviceKey";
    private static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    private static final String DEVICE_GUIDS = "deviceGuids";
    private static final String NAMES = "names";
    private static final String TIMESTAMP = "timestamp";
    private static final String SEPARATOR = ",";

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    protected final ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(50);

    private final URI restUri;
    private Client restClient;

    private HivePrincipal hivePrincipal;

    protected final ReadWriteLock connectionLock = new ReentrantReadWriteLock(true);
    protected final ReadWriteLock subscriptionsLock = new ReentrantReadWriteLock(true);

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> commandSubscriptionsStorage =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Future<?>> commandSubscriptionsResults = new ConcurrentHashMap<>();

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> notificationSubscriptionsStorage =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Future<?>> notificationSubscriptionResults = new ConcurrentHashMap<>();

    public RestAgent(URI restUri) {
        this.restUri = restUri;
    }

    public HivePrincipal getHivePrincipal() {
        connectionLock.readLock().lock();
        try {
            return hivePrincipal;
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    protected void beforeConnect() throws HiveException {
    }

    protected void doConnect() throws HiveException {
        this.restClient = JerseyClientBuilder.createClient();
        this.restClient.register(JsonRawProvider.class)
            .register(HiveEntityProvider.class)
            .register(CollectionProvider.class);
    }

    protected void afterConnect() throws HiveException {
    }

    protected void beforeDisconnect() {
        MoreExecutors.shutdownAndAwaitTermination(subscriptionExecutor, 1, TimeUnit.MINUTES);
        commandSubscriptionsResults.clear();
        notificationSubscriptionResults.clear();
    }

    protected void doDisconnect() {
        this.restClient.close();
    }

    protected void afterDisconnect() {
    }

    public final void connect() throws HiveException {
        connectionLock.writeLock().lock();
        try {
            beforeConnect();
            doConnect();
            afterConnect();
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    public final void disconnect() {
        connectionLock.writeLock().lock();
        try {
            beforeDisconnect();
            doDisconnect();
            afterDisconnect();
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    public void authenticate(final HivePrincipal hivePrincipal) throws HiveException {
        connectionLock.writeLock().lock();
        try {
            if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
                throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
            }
            this.hivePrincipal = hivePrincipal;
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    public final void close() {
        connectionLock.writeLock().lock();
        try {
            disconnect();
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unused")
    public boolean checkConnection() {
        try {
            final Response response = buildInvocation("/info", HttpMethod.GET, null, null, null, null).invoke();
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
    public <S> void execute(final String path, final String method, final Map<String, String> headers,
                            final S objectToSend, final JsonPolicyDef.Policy sendPolicy) throws HiveException {
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
    public void execute(final String path, final String method, final Map<String, String> headers,
                        final Map<String, Object> queryParams) throws HiveException {
        execute(path, method, headers, queryParams, null, null, null, null);
    }

    /**
     * Executes request with following params
     *
     * @param path   requested uri
     * @param method http method
     */
    public void execute(final String path, final String method) throws HiveException {
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
    public <R> R execute(final String path, final String method, final Map<String, String> headers,
                         final Map<String, Object> queryParams,
                         final Type typeOfR, final JsonPolicyDef.Policy receivePolicy) throws HiveException {
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
    public <R> R execute(final String path, final String method, final Map<String, String> headers, final Type typeOfR,
                         final JsonPolicyDef.Policy receivePolicy) throws HiveException {
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
    public <R> R executeForm(final String path, final Map<String, String> formParams, final Type typeOfR,
                             final JsonPolicyDef.Policy receivePolicy) throws HiveException {
        connectionLock.readLock().lock();
        try {
            final Response response = buildFormInvocation(path, formParams).invoke();
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
    public <S, R> R execute(final String path, final String method, final Map<String, String> headers,
                            final Map<String, Object> queryParams,
                            final S objectToSend, final Type typeOfR, final JsonPolicyDef.Policy sendPolicy,
                            final JsonPolicyDef.Policy receivePolicy) throws HiveException {
        connectionLock.readLock().lock();
        try {
            final Response response = buildInvocation(path, method, headers, queryParams, objectToSend, sendPolicy).invoke();
            return getEntity(response, typeOfR, receivePolicy);
        } catch (ProcessingException e) {
            throw new HiveException(Messages.INVOKE_TARGET_ERROR, e.getCause());
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    private <R> R getEntity(final Response response, final Type typeOfR,
                            final JsonPolicyDef.Policy receivePolicy) throws HiveException {
        final Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
        switch (statusFamily) {
            case SERVER_ERROR:
                throw new HiveServerException(response.getStatus());
            case CLIENT_ERROR:
                if (response.getStatus() == METHOD_NOT_ALLOWED.getStatusCode()) {
                    throw new InternalHiveClientException(METHOD_NOT_ALLOWED.getReasonPhrase(),
                                                          response.getStatus());
                }
                final ErrorMessage errorMessage = response.readEntity(ErrorMessage.class);
                throw new HiveClientException(errorMessage.getMessage(), response.getStatus());
            case SUCCESSFUL:
                if (typeOfR == null) {
                    return null;
                }
                if (receivePolicy == null) {
                    return response.readEntity(new GenericType<R>(typeOfR));
                } else {
                    final Annotation[] readAnnotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(receivePolicy)};
                    return response.readEntity(new GenericType<R>(typeOfR), readAnnotations);
                }
            default:
                throw new HiveException(Messages.UNKNOWN_RESPONSE);
        }
    }

    private Map<String, String> getAuthHeaders() {
        final Map<String, String> headers = new HashMap<>();
        final HivePrincipal hivePrincipal = getHivePrincipal();
        if (hivePrincipal != null) {
            if (hivePrincipal.isUser()) {
                final String decodedAuth = hivePrincipal.getPrincipal().getLeft() + ":" + hivePrincipal.getPrincipal().getRight();
                final String encodedAuth = Base64.encodeBase64String(decodedAuth.getBytes(UTF8_CHARSET));
                headers.put(HttpHeaders.AUTHORIZATION, USER_AUTH_SCHEMA + " " + encodedAuth);
            }
            if (hivePrincipal.isDevice()) {
                headers.put(DEVICE_ID_HEADER, hivePrincipal.getPrincipal().getLeft());
                headers.put(DEVICE_KEY_HEADER, hivePrincipal.getPrincipal().getRight());
            }
            if (hivePrincipal.isAccessKey()) {
                headers.put(HttpHeaders.AUTHORIZATION, KEY_AUTH_SCHEMA + " " + hivePrincipal.getPrincipal().getValue());
            }
        }
        return headers;
    }

    private WebTarget createTarget(final String path, final Map<String, Object> queryParams) {
        WebTarget target = restClient.target(restUri).path(path);
        if (queryParams != null) {
            for (final Map.Entry<String, Object> entry : queryParams.entrySet()) {
                final Object value = entry.getValue() instanceof Timestamp
                               ? TimestampAdapter.formatTimestamp((Timestamp) entry.getValue())
                               : entry.getValue();
                target = target.queryParam(entry.getKey(), value);
            }
        }
        return target;
    }

    private <S> Invocation buildInvocation(final String path,
                                           final String method,
                                           final Map<String, String> headers,
                                           final Map<String, Object> queryParams,
                                           final S objectToSend,
                                           final JsonPolicyDef.Policy sendPolicy) {
        final Invocation.Builder invocationBuilder = createTarget(path, queryParams)
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE);
        for (final Map.Entry<String, String> entry : getAuthHeaders().entrySet()) {
            invocationBuilder.header(entry.getKey(), entry.getValue());
        }
        if (headers != null) {
            for (final Map.Entry<String, String> customHeader : headers.entrySet()) {
                invocationBuilder.header(customHeader.getKey(), customHeader.getValue());
            }
        }
        if (objectToSend != null) {
            final Entity<S> entity;
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

    private WebTarget createTarget(final String path) {
        return createTarget(path, null);
    }

    private Invocation buildFormInvocation(final String path, final Map<String, String> formParams) throws HiveException {
        final Invocation.Builder invocationBuilder = createTarget(path).
            request().
            accept(MediaType.APPLICATION_JSON_TYPE).
            header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        for (final Map.Entry<String, String> entry : getAuthHeaders().entrySet()) {
            invocationBuilder.header(entry.getKey(), entry.getValue());
        }

        final Entity<Form> entity;
        if (formParams != null) {
            final Form f = new Form();
            for (final Map.Entry<String, String> entry : formParams.entrySet()) {
                f.param(entry.getKey(), entry.getValue());
            }
            entity = Entity.entity(f, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
            return invocationBuilder.build(HttpMethod.POST, entity);
        }
        throw new InternalHiveClientException(Messages.FORM_PARAMS_ARE_NULL);
    }

    public String subscribeForCommands(final SubscriptionFilter newFilter,
                                       final HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String subscriptionIdValue = UUID.randomUUID().toString();
            final SubscriptionDescriptor<DeviceCommand> descriptor = new SubscriptionDescriptor<>(handler, newFilter);
            commandSubscriptionsStorage.put(subscriptionIdValue, descriptor);

            final RestSubscription sub = new RestSubscription() {

                @Override
                protected void execute() throws HiveException {
                    final Map<String, Object> params = new HashMap<>();
                    params.put(WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    if (newFilter != null) {
                        params.put(TIMESTAMP, newFilter.getTimestamp());
                        params.put(NAMES, StringUtils.join(newFilter.getNames(), SEPARATOR));
                        params.put(DEVICE_GUIDS, StringUtils.join(newFilter.getUuids(), SEPARATOR));
                    }
                    final Type responseType = new TypeToken<List<CommandPollManyResponse>>() {
                    }.getType();
                    while (!Thread.currentThread().isInterrupted()) {
                        final List<CommandPollManyResponse> responses =
                            RestAgent.this.execute("/device/command/poll", HttpMethod.GET, null,
                                                   params, responseType, JsonPolicyDef.Policy.COMMAND_LISTED);
                        for (final CommandPollManyResponse response : responses) {
                            descriptor.handleMessage(response.getCommand());
                        }
                        if (!responses.isEmpty()) {
                            final Timestamp newTimestamp = responses.get(responses.size() - 1).getCommand().getTimestamp();
                            params.put(TIMESTAMP, newTimestamp);
                        }
                    }
                }
            };

            final Future<?> commandsSubscription = subscriptionExecutor.submit(sub);
            commandSubscriptionsResults.put(subscriptionIdValue, commandsSubscription);
            return subscriptionIdValue;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    public String subscribeForCommandsForDevice(final SubscriptionFilter newFilter,
                                                final HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String subscriptionIdValue = UUID.randomUUID().toString();
            final SubscriptionDescriptor<DeviceCommand> descriptor = new SubscriptionDescriptor<>(handler, newFilter);
            commandSubscriptionsStorage.put(subscriptionIdValue, descriptor);

            final RestSubscription sub = new RestSubscription() {

                @Override
                protected void execute() throws HiveException {
                    final Map<String, Object> params = new HashMap<>();
                    params.put(WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    if (newFilter != null) {
                        params.put(TIMESTAMP, newFilter.getTimestamp());
                        params.put(NAMES, StringUtils.join(newFilter.getNames(), SEPARATOR));
                    }
                    final Type responseType = new TypeToken<List<DeviceCommand>>() {
                    }.getType();
                    final String uri = String.format("/device/%s/command/poll", getHivePrincipal().getPrincipal().getLeft());
                    while (!Thread.currentThread().isInterrupted()) {
                        final List<DeviceCommand> responses =
                            RestAgent.this.execute(uri,
                                                   HttpMethod.GET,
                                                   null,
                                                   params, responseType, JsonPolicyDef.Policy.COMMAND_LISTED);
                        for (final DeviceCommand response : responses) {
                            descriptor.handleMessage(response);
                        }
                        if (!responses.isEmpty()) {
                            final Timestamp newTimestamp = responses.get(responses.size() - 1).getTimestamp();
                            params.put(TIMESTAMP, newTimestamp);
                        }
                    }
                }
            };

            final Future<?> commandsSubscription = subscriptionExecutor.submit(sub);
            commandSubscriptionsResults.put(subscriptionIdValue, commandsSubscription);
            return subscriptionIdValue;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    /**
     * Remove command subscription.
     */
    public void unsubscribeFromCommands(final String subscriptionId) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final Future<?> commandsSubscription = commandSubscriptionsResults.remove(subscriptionId);
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
                                           final HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final RestSubscription sub = new RestSubscription() {
                @Override
                protected void execute() throws HiveException {
                    DeviceCommand result = null;
                    final Map<String, Object> params = new HashMap<>();
                    params.put(WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    final Type responseType = new TypeToken<DeviceCommand>() {
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
            final SubscriptionDescriptor<DeviceNotification> descriptor = new SubscriptionDescriptor<>(handler, newFilter);
            notificationSubscriptionsStorage.put(subscriptionIdValue, descriptor);
            final RestSubscription sub = new RestSubscription() {
                @Override
                protected void execute() throws HiveException {
                    final Map<String, Object> params = new HashMap<>();
                    params.put(WAIT_TIMEOUT_PARAM, String.valueOf(TIMEOUT));
                    if (newFilter != null) {
                        params.put(TIMESTAMP, newFilter.getTimestamp());
                        params.put(NAMES, StringUtils.join(newFilter.getNames(), SEPARATOR));
                        params.put(DEVICE_GUIDS, StringUtils.join(newFilter.getUuids(), SEPARATOR));
                    }
                    final Type responseType = new TypeToken<List<NotificationPollManyResponse>>() {
                    }.getType();
                    while (!Thread.currentThread().isInterrupted()) {
                        final List<NotificationPollManyResponse> responses = RestAgent.this.execute(
                            "/device/notification/poll",
                            HttpMethod.GET,
                            null,
                            params,
                            responseType,
                            JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
                        for (final NotificationPollManyResponse response : responses) {
                            descriptor.handleMessage(response.getNotification());
                        }
                        if (!responses.isEmpty()) {
                            final Timestamp newTimestamp = responses.get(responses.size() - 1).getNotification().getTimestamp();
                            params.put(TIMESTAMP, newTimestamp);
                        }
                    }
                }
            };
            final Future<?> notificationsSubscription = subscriptionExecutor.submit(sub);
            notificationSubscriptionResults.put(subscriptionIdValue, notificationsSubscription);
            return subscriptionIdValue;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    /**
     * Remove command subscription for all available commands.
     */
    public void unsubscribeFromNotifications(final String subscriptionId) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final Future<?> notificationsSubscription = notificationSubscriptionResults.remove(subscriptionId);
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

    @SuppressWarnings("unused")
    public Timestamp getServerTimestamp() throws HiveException {
        return getInfo().getServerTimestamp();
    }

    @SuppressWarnings("unused")
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
