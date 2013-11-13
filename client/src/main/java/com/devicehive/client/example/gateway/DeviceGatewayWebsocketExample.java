package com.devicehive.client.example.gateway;


import com.devicehive.client.api.HiveDeviceGateway;
import com.devicehive.client.model.*;

import java.net.URI;

public class DeviceGatewayWebsocketExample extends DeviceGatewayExample {
    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        URI restUri = URI.create(args[0]);
        URI websocketUri = URI.create(args[1]);
        final HiveDeviceGateway hdg = new HiveDeviceGateway(restUri, websocketUri, Transport.PREFER_WEBSOCKET);
        DeviceGatewayWebsocketExample example = new DeviceGatewayWebsocketExample();
        example.example(hdg);
    }

}
