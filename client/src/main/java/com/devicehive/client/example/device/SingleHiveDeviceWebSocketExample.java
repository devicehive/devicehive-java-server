package com.devicehive.client.example.device;


import com.devicehive.client.api.SingleHiveDevice;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TODO
 */
public class SingleHiveDeviceWebSocketExample extends SingleHiveDeviceExample {

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        URI restUri = URI.create(args[0]);
        URI websocketUri = URI.create(args[1]);
        final SingleHiveDevice shd = new SingleHiveDevice(restUri, websocketUri, Transport.PREFER_WEBSOCKET);
        SingleHiveDeviceWebSocketExample example = new SingleHiveDeviceWebSocketExample();
        example.example(shd);
    }

}
