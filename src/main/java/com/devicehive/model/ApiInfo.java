package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * TODO JavaDoc
 */
public class ApiInfo {

    private String apiVersion;

    private Date serverTimestamp;

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
