package com.devicehive.model;

import com.devicehive.websockets.json.strategies.HiveAnnotations;

import java.util.Date;

import static com.devicehive.websockets.json.strategies.HiveAnnotations.WebsocketField;

/**
 * TODO JavaDoc
 */
public class ApiInfo {

    @WebsocketField
    private String apiVersion;

    @WebsocketField
    private Date serverTimestamp;

    @WebsocketField
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

    public Date getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(Date serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
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
