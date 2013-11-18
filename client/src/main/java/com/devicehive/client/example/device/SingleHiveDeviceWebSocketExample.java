package com.devicehive.client.example.device;


import com.devicehive.client.api.SingleHiveDevice;
import com.devicehive.client.model.Transport;

import java.net.URI;

public class SingleHiveDeviceWebSocketExample extends SingleHiveDeviceExample {

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        SingleHiveDeviceWebSocketExample example = new SingleHiveDeviceWebSocketExample();
        if (args.length < 2) {
            example.printUsage(System.out);
        } else {
            URI restUri = URI.create(args[0]);
            URI websocketUri = URI.create(args[1]);
            final SingleHiveDevice shd = new SingleHiveDevice(restUri, websocketUri, Transport.PREFER_WEBSOCKET);
            example.example(shd);
        }
    }

}
