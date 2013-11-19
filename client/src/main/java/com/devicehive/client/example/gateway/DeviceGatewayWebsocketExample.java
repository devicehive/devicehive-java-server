package com.devicehive.client.example.gateway;


import com.devicehive.client.api.gateway.HiveDeviceGateway;
import com.devicehive.client.model.Transport;

import java.net.URI;

public class DeviceGatewayWebsocketExample extends DeviceGatewayExample {
    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        DeviceGatewayWebsocketExample example = new DeviceGatewayWebsocketExample();
        if (args.length < 2) {
            example.printUsage(System.out);
        } else {
            URI restUri = URI.create(args[0]);
            URI websocketUri = URI.create(args[1]);
            final HiveDeviceGateway hdg = new HiveDeviceGateway(restUri, websocketUri, Transport.PREFER_WEBSOCKET);
            example.example(hdg);
        }
    }

}
