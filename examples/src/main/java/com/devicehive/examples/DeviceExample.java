package com.devicehive.examples;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.devicehive.client.HiveDevice;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceClass;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Equipment;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.Network;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.devicehive.view.DeviceView;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.constants.Constants.USE_SOCKETS;

public class DeviceExample extends Example {

    private static final String ID = "3d77f31c-bddd-443b-b11c-640946b0581a";
    private static final String KEY = "example_key";
    private static final String NAME = "Graphical Example Device";
    private static final String STATUS = "ONLINE";
    private static final String NETWORK_NAME = "VirtualLed Sample Network";
    private static final String DC_NAME = "Graphical";
    private static final String DC_VERSION = "1.0";
    private static final int DC_OFFLINE_TIMEOUT = 60 * 10;
    private static final String EQUIPMENT_NAME = "LED";
    private static final String EQUIPMENT_TYPE = "Controllable LED";
    private static final String LED_COMMAND = "LED";
    private static final String LED_STATE = "state";
    private static final String SPECIAL_EQUIPMENT_NOTIFICATION = "equipment";
    private final HiveDevice hiveDevice;
    private final DeviceView view;
    private volatile boolean deviceState = false;


    public DeviceExample(PrintStream err, PrintStream out, String... args)
        throws HiveException, ExampleException, IOException {
        super(out, args);
        CommandLine commandLine = getCommandLine();
        hiveDevice = HiveFactory.createDevice(getServerUrl(), commandLine.hasOption(USE_SOCKETS));
        view = new DeviceView(hiveDevice);
    }

    public static void main(String... args) {
        try {
            Example deviceExample = new DeviceExample(System.err, System.out, args);
            deviceExample.run();
        } catch (HiveException | ExampleException | IOException e) {
            System.err.print(e);
        }

    }

    @Override
    public Options makeOptionsSet() {
        return new Options();
    }

    private Device createDevice() throws ExampleException {
        Device device = new Device();
        device.setId(ID);
        device.setKey(KEY);
        device.setName(NAME);
        device.setStatus(STATUS);
        Network existing = new Network();
        existing.setName(NETWORK_NAME);
        device.setNetwork(existing);
        DeviceClass dc = new DeviceClass();
        dc.setName(DC_NAME);
        dc.setVersion(DC_VERSION);
        dc.setPermanent(true);
        dc.setOfflineTimeout(DC_OFFLINE_TIMEOUT);

        Set<Equipment> equipmentSet = new HashSet<Equipment>() {
            {
                Equipment equipment = new Equipment();
                equipment.setName(EQUIPMENT_NAME);
                equipment.setType(EQUIPMENT_TYPE);
                equipment.setCode(LED_COMMAND);
            }

            private static final long serialVersionUID = -7409380697000528798L;
        };
        dc.setEquipment(equipmentSet);
        device.setDeviceClass(dc);
        return device;
    }

    @Override
    public void run() throws HiveException, ExampleException, IOException {
        Device device = createDevice();
        hiveDevice.registerDevice(device);
        hiveDevice.authenticate(device.getId(), device.getKey());
        Timestamp serverTimestamp = hiveDevice.getInfo().getServerTimestamp();
        HiveMessageHandler<DeviceCommand> commandsHandler = new HiveMessageHandler<DeviceCommand>() {
            @Override
            public void handle(DeviceCommand command) {
                if (command.getCommand().equals(LED_COMMAND)) {
                    JsonStringWrapper jsonString = command.getParameters();
                    JsonObject json = (JsonObject) new JsonParser().parse(jsonString.toString());
                    boolean state = json.get(LED_STATE).getAsBoolean();
                    DeviceNotification equipment = null;
                    if (state != deviceState) {
                        equipment = new DeviceNotification();
                        equipment.setNotification(SPECIAL_EQUIPMENT_NOTIFICATION);
                        JsonObject equipmentJson = new JsonObject();
                        equipmentJson.addProperty(SPECIAL_EQUIPMENT_NOTIFICATION, LED_COMMAND);
                        equipmentJson.addProperty(LED_STATE, state);
                        equipment.setParameters(new JsonStringWrapper(equipmentJson.toString()));
                    }
                    if (state) {
                        view.setGreen();
                    } else {
                        view.setRed();
                    }
                    deviceState = state;

                    try {
                        if (equipment != null) {
                            hiveDevice.insertNotification(equipment);
                        }
                    } catch (HiveException e) {
                        if (state) {
                            view.setRed();
                        } else {
                            view.setGreen();
                        }
                        deviceState = !state;
                        while (!Thread.currentThread().isInterrupted()) {
                        }
                    } finally {
                        command.setStatus("Proceed");
                        command.setResult(new JsonStringWrapper("{status: \"Ok\"}"));
                        boolean sent = false;
                        while (!sent) {
                            try {
                                hiveDevice.updateCommand(command);
                                sent = true;
                            } catch (HiveException e) {
                                while (!Thread.currentThread().isInterrupted()) {
                                }
                            }
                        }
                    }
                }
            }
        };
        hiveDevice.subscribeForCommands(serverTimestamp, commandsHandler);
    }
}
