package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.sql.Timestamp;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_INFO;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

/**
 * TODO JavaDoc
 */
public class ApiInfo implements HiveEntity {

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    private String apiVersion;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    private Timestamp serverTimestamp;

    @JsonPolicyDef(WEBSOCKET_SERVER_INFO)
    private String webSocketServerUrl;

    @JsonPolicyDef(REST_SERVER_INFO)
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
        return serverTimestamp;
    }

    public void setServerTimestamp(Timestamp serverTimestamp) {
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
