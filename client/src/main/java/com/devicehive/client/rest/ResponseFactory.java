package com.devicehive.client.rest;


import com.devicehive.client.config.Preferences;
import com.devicehive.client.model.CredentialsStorage;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.glassfish.jersey.internal.util.Base64;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.devicehive.client.model.CredentialsStorage.Role.DEVICE;
import static com.devicehive.client.model.CredentialsStorage.Role.USER;
import static javax.ws.rs.core.Response.Status.*;

public class ResponseFactory {

    public static <T> T responseGet(WebTarget target, Class<T> resultType, Annotation readPolicyAnnotation) {
        Map<String, String> headers = getAuthHeaders();
        return response(headers, target, HttpMethod.GET, resultType, null, null, readPolicyAnnotation);
    }

    public static <T> T responsePost(WebTarget target, Class<T> resultType, T entityToPost) {
        Map<String, String> headers = getAuthHeaders();
        return response(headers, target, HttpMethod.POST, resultType, entityToPost, null, null);
    }

    public static boolean responseDelete(WebTarget target) {
        Map<String, String> headers = getAuthHeaders();
        response(headers, target, HttpMethod.DELETE, null, null, null, null);
        return true;
    }

    public static <T> boolean responseUpdate(WebTarget target, T entityToUpdate) {
        Map<String, String> headers = getAuthHeaders();
        response(headers, target, HttpMethod.PUT, null, entityToUpdate, null, null);
        return true;
    }

    private static Map<String, String> getAuthHeaders() {
        CredentialsStorage currentUserInfo = Preferences.getCurrentUserInfoStorage();
        Map<String, String> headers;
        if (currentUserInfo == null) {
            headers = Collections.emptyMap();
        } else {
            if (currentUserInfo.getRole().equals(USER)) {
                headers = getUserAuthHeaders(currentUserInfo.getId(), currentUserInfo.getCredentials());
            } else if (currentUserInfo.getRole().equals(DEVICE)) {
                headers = getDeviceAuthHeaders(currentUserInfo.getId(), currentUserInfo.getCredentials());
            } else {
                headers = getKeyAuthHeaders(currentUserInfo.getId());
            }
        }
        return headers;
    }

    private static Map<String, String> getUserAuthHeaders(String login, String password) {
        Map<String, String> headers = new HashMap<>(1);
        String decodedAuth = login + ":" + password;
        String encodedAuth = Base64.encodeAsString(decodedAuth);
        headers.put(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        return headers;
    }

    private static Map<String, String> getDeviceAuthHeaders(String id, String key) {
        Map<String, String> headers = new HashMap<>(2);
        headers.put("Auth-DeviceID", id);
        headers.put("Auth-DeviceKey", key);
        return headers;
    }

    private static Map<String, String> getKeyAuthHeaders(String key) {
        Map<String, String> headers = new HashMap<>(1);
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + key);
        return headers;
    }

    private static <T> T response(Map<String, String> headers,
                                  WebTarget target,
                                  String method,
                                  Class<T> entityType,
                                  T entityObject,
                                  Annotation writeAnnotation,
                                  Annotation readAnnotation) {
        Invocation.Builder invocationBuilder = target.
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).
                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        for (String headerName : headers.keySet()) {
            invocationBuilder.header(headerName, headers.get(headerName));
        }
        Response response;
        Entity<T> entity;
        Annotation[] writeAnnotations = {writeAnnotation};
        switch (method) {
            case HttpMethod.POST:
                if (writeAnnotation == null) {
                    entity = Entity.entity(entityObject, MediaType.APPLICATION_JSON_TYPE);
                } else {
                    entity = Entity.entity(entityObject, MediaType.APPLICATION_JSON_TYPE, writeAnnotations);
                }
                response = invocationBuilder.buildPost(entity).invoke();
                break;
            case HttpMethod.PUT:
                if (writeAnnotation == null) {
                    entity = Entity.entity(entityObject, MediaType.APPLICATION_JSON_TYPE);
                } else {
                    entity = Entity.entity(entityObject, MediaType.APPLICATION_JSON_TYPE, writeAnnotations);
                }
                response = invocationBuilder.buildPut(entity).invoke();
                break;
            default:
                response = invocationBuilder.build(method).invoke();
        }
        Family statusFamily = response.getStatusInfo().getFamily();
        switch (statusFamily) {
            case SERVER_ERROR:
                throw new HiveServerException(response.getStatus());
            case CLIENT_ERROR:
                if (response.getStatus() == METHOD_NOT_ALLOWED.getStatusCode()) {
                    throw new HiveException(METHOD_NOT_ALLOWED.getReasonPhrase(), response.getStatus());
                }
                Gson gson = new Gson();
                JsonObject jsonResponse = gson.toJsonTree(response.getEntity()).getAsJsonObject();
                String reason = jsonResponse.get("message").getAsString();
                if (response.getStatus() == BAD_REQUEST.getStatusCode()) {
                    throw new HiveException(reason, response.getStatus());
                }
                throw new HiveClientException(reason, response.getStatus());
            case SUCCESSFUL:
                if (entityType == null) {
                    return null;
                }
                if (readAnnotation == null) {
                    return response.readEntity(entityType);
                }
                Annotation[] readAnnotations = {readAnnotation};
                return response.readEntity(entityType, readAnnotations);
        }
        return null;
    }


}
