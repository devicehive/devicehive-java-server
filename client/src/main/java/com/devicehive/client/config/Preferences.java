package com.devicehive.client.config;


import com.devicehive.client.model.CredentialsStorage;

public class Preferences {
    private static String REST_SERVER_URL;
    private static String WEBSOCKET_SERVER_URL;
    private static CredentialsStorage currentUserInfoStorage;

    public static CredentialsStorage getCurrentUserInfoStorage() {
        return currentUserInfoStorage;
    }

    public static void setCurrentUserInfoStorage(CredentialsStorage currentUserInfoStorage) {
        Preferences.currentUserInfoStorage = currentUserInfoStorage;
    }

    public static String getRestServerUrl() {
        return REST_SERVER_URL;
    }

    public static void setRestServerUrl(String newRestServerUrl) {
        Preferences.REST_SERVER_URL = newRestServerUrl;
    }

    public static String getWebSocketServerUrl() {
        return WEBSOCKET_SERVER_URL;
    }

    public static void setWebSocketServerUrl(String newWebsocketServerUrl) {
        Preferences.WEBSOCKET_SERVER_URL = newWebsocketServerUrl;
    }
}
