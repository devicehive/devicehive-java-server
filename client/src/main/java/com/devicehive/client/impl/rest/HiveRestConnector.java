package com.devicehive.client.impl.rest;


import com.devicehive.client.impl.context.Constants;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.impl.json.strategies.JsonPolicyApply;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.impl.rest.providers.CollectionProvider;
import com.devicehive.client.impl.rest.providers.HiveEntityProvider;
import com.devicehive.client.impl.rest.providers.JsonRawProvider;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.ErrorMessage;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;

/**
 * Part of client that creates requests based on required parameters (set by user) and parses responses into model
 * classes representation
 */
public class HiveRestConnector {
    private static final String USER_AUTH_SCHEMA = "Basic";
    private static final String KEY_AUTH_SCHEMA = "Bearer";
    private static Logger logger = LoggerFactory.getLogger(HiveRestConnector.class);
    private final URI uri;
    private final Client restClient;
    private volatile HivePrincipal hivePrincipal;


    /**
     * Creates client connected to the given REST URL. All state is kept in the hive context.
     *
     * @param uri URI of RESTful service
     */
    public HiveRestConnector(URI uri) throws HiveException {
        this.uri = uri;
        restClient = getClient();
    }

    private static JerseyClient getClient() {
        JerseyClient client = JerseyClientBuilder.createClient();
        return client.register(JsonRawProvider.class)
                .register(HiveEntityProvider.class)
                .register(CollectionProvider.class);

    }


    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }

    public void close() {
        restClient.close();
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
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such classes
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
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such classes
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
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such classes
     * @param receivePolicy policy that declares exclusion strategy for received object
     * @return instance of TypeOfR or null
     */
    public synchronized <R> R executeForm(String path, Map<String, String> formParams, Type typeOfR,
                                          JsonPolicyDef.Policy receivePolicy) throws HiveException {
        try {
            Response response = buildFormInvocation(path, formParams).invoke();
            return getEntity(response, typeOfR, receivePolicy);
        } catch (ProcessingException e) {
            throw new HiveException("Error invoking the target", e.getCause());
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
     * @param typeOfR       type of response. Should be a class that implements hive entity or a collection of such classes
     * @param sendPolicy    policy that declares exclusion strategy for sending object
     * @param receivePolicy policy that declares exclusion strategy for received object
     * @return instance of TypeOfR or null
     */
    public <S, R> R execute(String path, String method, Map<String, String> headers,
                            Map<String, Object> queryParams,
                            S objectToSend, Type typeOfR, JsonPolicyDef.Policy sendPolicy,
                            JsonPolicyDef.Policy receivePolicy) throws HiveException {
        try {
            Response response = buildInvocation(path, method, headers, queryParams, objectToSend, sendPolicy).invoke();
            return getEntity(response, typeOfR, receivePolicy);
        } catch (ProcessingException e) {
            throw new HiveException("Error invoking the target", e.getCause());
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
        if (hivePrincipal != null) {
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
        WebTarget target = restClient.target(uri).path(path);
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

    private <S> Invocation buildInvocation(String path, String method, Map<String, String> headers, Map<String,
            Object> queryParams, S objectToSend, JsonPolicyDef.Policy sendPolicy) {
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


}
