package com.devicehive.client.example.device;

import com.devicehive.client.api.SingleHiveDevice;
import com.devicehive.client.model.Transport;

import java.net.URI;

public class SingleHiveDeviceRestExample extends SingleHiveDeviceExample {

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        SingleHiveDeviceRestExample example = new SingleHiveDeviceRestExample();
        if (args.length < 2) {
            example.printUsage(System.out);
        } else {
            URI restUri = URI.create(args[0]);
            URI websocketUri = URI.create(args[1]);
            final SingleHiveDevice shd = new SingleHiveDevice(restUri, websocketUri, Transport.REST_ONLY);
            example.example(shd);
        }
    }


}
