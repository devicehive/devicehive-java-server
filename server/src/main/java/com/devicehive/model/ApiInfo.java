package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_INFO;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

/**
 * Represents meta-information about the current API. For more details see <a href="http://www.devicehive.com/restful#Reference/ApiInfo">ApiInfo</a>
 */
public class ApiInfo implements HiveEntity {


    private static final long serialVersionUID = -4899398629379606180L;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    private String apiVersion;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    @Temporal(TemporalType.TIMESTAMP)
    private Date serverTimestamp;

    @JsonPolicyDef(REST_SERVER_INFO)
    private String webSocketServerUrl;

    @JsonPolicyDef(WEBSOCKET_SERVER_INFO)
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
        return ObjectUtils.cloneIfPossible(serverTimestamp);
    }

    public void setServerTimestamp(Date serverTimestamp) {
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
