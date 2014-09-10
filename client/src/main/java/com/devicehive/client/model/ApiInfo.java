package com.devicehive.client.model;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

/**
 * Represents meta-information about the current API.
 * For more details see <a href="http://www.devicehive.com/restful#Reference/ApiInfo">ApiInfo</a>
 */
public class ApiInfo implements HiveEntity {

    private static final long serialVersionUID = 2607988877048211220L;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiInfo{");
        sb.append("apiVersion='").append(apiVersion).append('\'');
        sb.append(", serverTimestamp=").append(serverTimestamp);
        sb.append(", webSocketServerUrl='").append(webSocketServerUrl).append('\'');
        sb.append(", restServerUrl='").append(restServerUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
