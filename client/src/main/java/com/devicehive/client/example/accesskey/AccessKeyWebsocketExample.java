package com.devicehive.client.example.accesskey;


import com.devicehive.client.model.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.net.URI;

public class AccessKeyWebsocketExample extends AccessKeyExample {
    private static Logger logger = LoggerFactory.getLogger(AccessKeyWebsocketExample.class);

    public AccessKeyWebsocketExample(PrintStream out) {
        super(out);
    }

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        AccessKeyWebsocketExample example = new AccessKeyWebsocketExample(System.out);
        if (args.length < 1) {
            example.printUsage();
        } else {
            URI rest = URI.create(args[0]);
            try {
                example.init(rest, Transport.PREFER_WEBSOCKET);
                System.out.println("--- Device example ---");
                example.deviceExample(rest);
                System.out.println("--- Commands example ---");
                example.commandsExample(rest);
                System.out.println("--- Notifications example ---");
                example.notificationsExample(rest);
                System.out.println("--- Network example ---");
                example.networkExample(rest);
            } finally {
                example.close();
            }
        }
    }


}
