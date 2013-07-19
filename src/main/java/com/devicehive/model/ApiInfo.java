package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

/**
 * TODO JavaDoc
 */
public class ApiInfo implements HiveEntity {

    @JsonPolicyDef(WEBSOCKET_SERVER_INFO)
    private String apiVersion;

    @JsonPolicyDef(WEBSOCKET_SERVER_INFO)
    private Date serverTimestamp;

    @JsonPolicyDef(WEBSOCKET_SERVER_INFO)
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
