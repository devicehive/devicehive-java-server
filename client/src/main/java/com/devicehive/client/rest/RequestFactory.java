package com.devicehive.client.rest;

import com.devicehive.client.config.Preferences;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.util.Map;

public class RequestFactory {

    private static final Client CLIENT = HiveClientFactory.getClient();

    public static WebTarget request(String path, Map<String, Object> queryParams) {
        WebTarget target = CLIENT.target(Preferences.getRestServerUrl()).path(path);
        if (queryParams != null) {
            for (String paramName : queryParams.keySet()) {
                target.queryParam(paramName, queryParams.get(paramName));
            }
        }
        return target;
    }
}
