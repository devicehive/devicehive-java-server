package com.devicehive.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * TODO JavaDoc
 */
@XmlRootElement
public class ApiInfo {
    public String apiVersion;
    public Date serverTimestamp;
    public String webSocketServerUrl;

    public ApiInfo() {
        apiVersion = Version.VERSION;
        serverTimestamp = new Date();
        webSocketServerUrl = "localhost";
    }
}
