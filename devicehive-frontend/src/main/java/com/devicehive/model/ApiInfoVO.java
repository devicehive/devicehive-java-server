package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_INFO;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

/**
 * Represents meta-information about the current API. For more details see <a href="http://www.devicehive.com/restful#Reference/ApiInfo">ApiInfoVO</a>
 */
public class ApiInfoVO implements HiveEntity {
    private static final long serialVersionUID = -2424398429391606180L;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO})
    private String apiVersion;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    private Date serverTimestamp;

    public ApiInfoVO() {
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
}
