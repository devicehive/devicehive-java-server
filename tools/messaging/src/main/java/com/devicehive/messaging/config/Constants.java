package com.devicehive.messaging.config;


import java.net.URI;

public class Constants {

    public static final URI REST_URI = URI.create("http://localhost:8080/hive/rest");

    public static final int ITEMS_INCREMENT = 10;
    public static final int MAX_DEVICES = 10;
    public static final int MAX_CLIENTS = 10;
    public static final int TIME_INCREMENT = 10;
    public static final boolean USE_SOCKETS = true;
}
