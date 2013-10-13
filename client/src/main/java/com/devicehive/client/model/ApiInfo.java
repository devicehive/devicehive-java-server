package com.devicehive.client.model;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

/**
 * TODO JavaDoc
 */
public class ApiInfo implements HiveEntity {

    private String apiVersion;

    private Timestamp serverTimestamp;

    private String webSocketServerUrl;

    private String restServerUrl;

    public ApiInfo() {
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Timestamp getServerTimestamp() {
        return ObjectUtils.cloneIfPossible(serverTimestamp);
    }

    public void setServerTimestamp(Timestamp serverTimestamp) {
        this.serverTimestamp = ObjectUtils.cloneIfPossible(serverTimestamp);
    }

    public String getWebSocketServerUrl() {
        return webSocketServerUrl;
    }

    public void setWebSocketServerUrl(String webSocketServerUrl) {
        this.webSocketServerUrl = webSocketServerUrl;
    }


    public String getRestServerUrl() {
        return restServerUrl;
    }

    public void setRestServerUrl(String restServerUrl) {
        this.restServerUrl = restServerUrl;
    }
}
