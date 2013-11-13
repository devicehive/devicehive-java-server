package com.devicehive.client.example.accesskey;


import com.devicehive.client.api.*;
import com.devicehive.client.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class AccessKeyWebsocketExample extends AccessKeyExample {
    private static Logger logger = LoggerFactory.getLogger(AccessKeyWebsocketExample.class);
    private Client client;

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        AccessKeyWebsocketExample example = new AccessKeyWebsocketExample();
        URI rest = URI.create(args[0]);
        URI websocket = URI.create(args[1]);
        try {
            example.init(rest, websocket, Transport.PREFER_WEBSOCKET);
            System.out.println("--- Device example ---");
            example.deviceExample(rest, websocket);
            System.out.println("--- Commands example ---");
            example.commandsExample(rest, websocket);
            System.out.println("--- Notifications example ---");
            example.notificationsExample(rest, websocket);
            System.out.println("--- Network example ---");
            example.networkExample(rest, websocket);
        } finally {
            example.close();
        }
    }


}
