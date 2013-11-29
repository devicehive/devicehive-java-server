package com.devicehive.client.example.gateway;


import com.devicehive.client.api.gateway.HiveDeviceGateway;
import com.devicehive.client.model.Transport;

import java.net.URI;

public class DeviceGatewayWebsocketExample extends DeviceGatewayExample {
    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     */
    public static void main(String... args) {
        DeviceGatewayWebsocketExample example = new DeviceGatewayWebsocketExample();
        if (args.length < 1) {
            example.printUsage(System.out);
        } else {
            URI restUri = URI.create(args[0]);
            final HiveDeviceGateway hdg = new HiveDeviceGateway(restUri, Transport.PREFER_WEBSOCKET);
            example.example(hdg);
        }
    }

}
