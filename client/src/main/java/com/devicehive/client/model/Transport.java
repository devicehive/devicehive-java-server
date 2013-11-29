package com.devicehive.client.model;

/**
 * Defines which protocol should be used: websocket or RESTful
 */
public enum Transport {

    AUTO(1, 2), PREFER_WEBSOCKET(1, 2), REST_ONLY(1, 0);

    private int restPriority;
    private int websocketPriority;


    private Transport(int restPriority, int websocketPriority) {
        this.restPriority = restPriority;
        this.websocketPriority = websocketPriority;
    }

    public int getRestPriority() {
        return restPriority;
    }

    public int getWebsocketPriority() {
        return websocketPriority;
    }
}
