package com.devicehive.client.model;


public enum Role {
    DEVICE("/device"), USER("/client");


    private String websocketSubPath;

    Role(String websocketSubPath) {
        this.websocketSubPath = websocketSubPath;
    }

    public String getWebsocketSubPath() {
        return websocketSubPath;
    }
}
