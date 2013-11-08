package com.devicehive.client.context;


import com.devicehive.client.config.Constants;
import com.devicehive.client.json.strategies.JsonPolicyApply;
import com.devicehive.client.json.strategies.JsonPolicyDef;
import com.devicehive.client.model.ErrorMessage;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.rest.HiveClientFactory;
import com.google.common.collect.Maps;
import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;

public class HiveRestClient implements Closeable {
    private static Logger logger = LoggerFactory.getLogger(HiveRestClient.class);
    private static final String USER_AUTH_SCHEMA = "Basic";
    private static final String KEY_AUTH_SCHEMA = "Bearer";
    private final URI rest;
    private final Client restClient;
    private final HiveContext hiveContext;

    public HiveRestClient(URI rest, HiveContext hiveContext) {
        this.rest = rest;
        this.hiveContext = hiveContext;
        restClient = HiveClientFactory.getClient();
    }

    @Override
    public void close() throws IOException {
        restClient.close();
    }

    public WebTarget createTarget(String path) {
        return createTarget(path, null);
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = Maps.newHashMap();

        HivePrincipal principal = hiveContext.getHivePrincipal();
        if (principal != null) {
            if (principal.getUser() != null) {
                String decodedAuth = principal.getUser().getLeft() + ":" + principal.getUser().getRight();
                String encodedAuth = Base64.encodeAsString(decodedAuth);
                headers.put(HttpHeaders.AUTHORIZATION, USER_AUTH_SCHEMA + " " + encodedAuth);
            }
            if (principal.getDevice() != null) {
                headers.put(Constants.DEVICE_ID_HEADER, principal.getDevice().getLeft());
                headers.put(Constants.DEVICE_KEY_HEADER, principal.getDevice().getRight());
            }
            if (principal.getAccessKey() != null) {
                headers.put(HttpHeaders.AUTHORIZATION, KEY_AUTH_SCHEMA + " " + principal.getAccessKey());
            }
        }
        return headers;
    }

    private WebTarget createTarget(String path, Map<String, Object> queryParams) {
        WebTarget target = restClient.target(rest).path(path);
        if (queryParams != null) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                target = target.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }

    private <S> Invocation buildInvocation(String path, String method, Map<String, String> headers, Map<String,
            Object> queryParams, S objectToSend, JsonPolicyDef.Policy sendPolicy) {
        Invocation.Builder invocationBuilder = createTarget(path, queryParams).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).
                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
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

    public <S> void execute(String path, String method, Map<String, String> headers, S objectToSend,
                            JsonPolicyDef.Policy sendPolicy) {
        execute(path, method, headers, null, objectToSend, null, sendPolicy, null);
    }

    public void execute(String path, String method, Map<String, String> headers, Map<String, Object> queryParams) {
        execute(path, method, headers, queryParams, null, null, null, null);
    }

    public void execute(String path, String method) {
        execute(path, method, null, null, null, null, null, null);
    }

    public <R> R execute(String path, String method, Map<String, String> headers, Map<String, Object> queryParams,
                         Type typeOfR,
                         JsonPolicyDef.Policy receivePolicy) {
        return execute(path, method, headers, queryParams, null, typeOfR, null, receivePolicy);
    }

    public <R> R execute(String path, String method, Map<String, String> headers, Type typeOfR,
                         JsonPolicyDef.Policy receivePolicy) {
        return execute(path, method, headers, null, null, typeOfR, null, receivePolicy);
    }

    public <S, R> R execute(String path, String method, Map<String, String> headers, Map<String, Object> queryParams,
                            S objectToSend, Type typeOfR, JsonPolicyDef.Policy sendPolicy,
                            JsonPolicyDef.Policy receivePolicy) {

        Response response = buildInvocation(path, method, headers, queryParams, objectToSend, sendPolicy).invoke();
        Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
        switch (statusFamily) {
            case SERVER_ERROR:
                throw new HiveServerException(response.getStatus());
            case CLIENT_ERROR:
                if (response.getStatus() == METHOD_NOT_ALLOWED.getStatusCode()) {
                    throw new InternalHiveClientException(METHOD_NOT_ALLOWED.getReasonPhrase(), response.getStatus());
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
                throw new HiveException("Unknown response");
        }

    }

    public <S, R> R executeAsync(String path, String method, Map<String, String> headers,
                                 Map<String, Object> queryParams, S objectToSend, Type typeOfR,
                                 JsonPolicyDef.Policy sendPolicy, JsonPolicyDef.Policy receivePolicy) {

        Future<Response> futureResponse = buildAsyncInvocation(path, method, headers, queryParams, objectToSend,
                sendPolicy);
        try {
            Response response = futureResponse.get(1L, TimeUnit.MINUTES);
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
                    throw new HiveException("Unknown response");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("task cancelled for path: {}", path);
            throw new InternalHiveClientException("task cancelled", e);
        } catch (TimeoutException e) {
            throw new HiveServerException("Server does not response!", INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private <S> Future<Response> buildAsyncInvocation(String path, String method, Map<String, String> headers,
                                                      Map<String, Object> queryParams, S objectToSend,
                                                      JsonPolicyDef.Policy sendPolicy) {
        Invocation.Builder invocationBuilder = createTarget(path, queryParams).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).
                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
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
            return invocationBuilder.async().method(method, entity);
        } else {
            return invocationBuilder.async().method(method);
        }
    }

    public <R> R executeForm(String path, Map<String, String> formParams, Type typeOfR,
                             JsonPolicyDef.Policy receivePolicy) {
        Response response = buildFormInvocation(path, formParams).invoke();
        Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
        switch (statusFamily) {
            case SERVER_ERROR:
                throw new HiveServerException(response.getStatus());
            case CLIENT_ERROR:
                if (response.getStatus() == METHOD_NOT_ALLOWED.getStatusCode()) {
                    throw new InternalHiveClientException(METHOD_NOT_ALLOWED.getReasonPhrase(), response.getStatus());
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
                throw new HiveException("Unknown response");
        }

    }

    private Invocation buildFormInvocation(String path, Map<String, String> formParams) {
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
        throw new InternalHiveClientException("form params cannot be null!");

    }
}
