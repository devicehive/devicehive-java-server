package com.devicehive.examples;


import com.google.gson.JsonObject;

import com.devicehive.client.CommandsController;
import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.devicehive.view.ClientView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client example represents base client features. It sends commands and receive notifications.
 */

public class ClientExample {

    private static final Logger logger = LoggerFactory.getLogger(ClientExample.class);
    private static final String LOGIN = "dhadmin";
    private static final String PASSWORD = "dhadmin_#911";
    private static final String LED_COMMAND = "LED";
    private static final String LED_STATE = "state";
    private static final String DEVICE_ID = "3d77f31c-bddd-443b-b11c-640946b0581a";
    private static final DeviceCommand TURN_ON = new DeviceCommand();
    private static final DeviceCommand TURN_OFF = new DeviceCommand();
    private final ClientView view;
    private HiveClient hiveClient;
    private ExecutorService main = Executors.newSingleThreadExecutor();

    static {
        TURN_ON.setCommand(LED_COMMAND);
        TURN_OFF.setCommand(LED_COMMAND);
        JsonObject params = new JsonObject();
        params.addProperty(LED_STATE, true);
        TURN_ON.setParameters(new JsonStringWrapper(params.toString()));
        params.remove(LED_STATE);
        params.addProperty(LED_STATE, false);
        TURN_OFF.setParameters(new JsonStringWrapper(params.toString()));
    }

    /**
     * Constructor. Creates hiveClient instance.
     */
    public ClientExample() {
        view = new ClientView(this);
    }

    /**
     * Entrance point.
     *
     * @param args command line arguments
     */
    public static void main(String... args) {
        ClientExample clientExample = new ClientExample();
    }

    /**
     * Shows how to authorize using access key or user. Subscribes for the notifications. Sends a dummy command to all
     * available devices every 10 seconds. The task runs for 10 minutes.
     */
    public void run(URI url, boolean useSockets) throws HiveException, ExampleException, IOException {
        hiveClient = HiveFactory
            .createClient(url, useSockets);
        Thread.currentThread().setName("run");
        hiveClient.authenticate(LOGIN, PASSWORD);
        HiveMessageHandler<DeviceNotification> notificationsHandler = new HiveMessageHandler<DeviceNotification>() {
            @Override
            public void handle(DeviceNotification notification) {
                System.out.print("Notification received: {}" + notification);
            }
        };
        Set<String> uuids = new

            HashSet<String>() {
                {
                    add(DEVICE_ID);
                }

                private static final long serialVersionUID = -8028742640202067195L;
            };
        SubscriptionFilter filter = new SubscriptionFilter(uuids, null, null);
        hiveClient.getNotificationsController().subscribeForNotifications(filter, notificationsHandler);
    }

    public ActionListener createTurnOnListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    CommandsController cc = hiveClient.getCommandsController();
                    cc.insertCommand(DEVICE_ID, TURN_ON, null);
                } catch (HiveException e1) {
                    logger.error(e1.getMessage(), e1);
                }
            }
        };
    }

    public ActionListener createTurnOffListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    CommandsController cc = hiveClient.getCommandsController();
                    cc.insertCommand(DEVICE_ID, TURN_OFF, null);
                } catch (HiveException e1) {
                    logger.error(e1.getMessage(), e1);
                }
            }
        };
    }

    public void close() {
        main.shutdownNow();
        hiveClient.close();
    }
}
