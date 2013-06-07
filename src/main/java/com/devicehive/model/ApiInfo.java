package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * TODO JavaDoc
 */
public class ApiInfo {

    @SerializedName("apiVersion")
    private String apiVersion;

    @SerializedName("serverTimestamp")
    private Date serverTimestamp;

    @SerializedName("webSocketServerUrl")
    private String webSocketServerUrl;

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
}
