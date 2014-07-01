package com.devicehive.examples;


import com.devicehive.client.HiveDevice;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceClass;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.Equipment;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.Network;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.devicehive.view.DeviceView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.devicehive.constants.Constants.USE_SOCKETS;

public class DeviceExample extends Example {
    private static final String ID = "3d77f31c-bddd-443b-b11c-640946b0581a";
    private static final String KEY = "example_key";
    private static final String NAME = "Graphical Example Device";
    private static final String STATUS = "ONLINE";
    private static final String NETWORK_NAME = "VirtualLed Sample Network";
    private static final String DC_NAME = "Graphical";
    private static final String DC_VERSION = "1.0";
    private static final int DC_OFFLINE_TIMEOUT = 10;
    private static final String EQUIPMENT_NAME = "LED";
    private static final String EQUIPMENT_TYPE = "Controllable LED";
    private static final String LED_COMMAND = "LED";
    private static final String LED_STATE = "state";
    private final HiveDevice hiveDevice;
    private final DeviceView view;
    private volatile boolean deviceState = false;


    public DeviceExample(PrintStream err, PrintStream out, String... args)
            throws HiveException, ExampleException, IOException {
        super(out, args);
        CommandLine commandLine = getCommandLine();
        hiveDevice = HiveFactory.createDevice(getServerUrl(), commandLine.hasOption(USE_SOCKETS),
                Example.impl, Example.impl);
        view = new DeviceView();
    }

    public static void main(String... args) {
        Example deviceExample;
        try {
            deviceExample = new DeviceExample(System.err, System.out, args);
            deviceExample.run();
        } catch (HiveException | ExampleException | IOException e) {
            System.err.println(e.getMessage());
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

        Set<Equipment> equipmentSet = new HashSet<Equipment>() {{
            Equipment equipment = new Equipment();
            equipment.setName(EQUIPMENT_NAME);
            equipment.setType(EQUIPMENT_TYPE);
            equipment.setCode(LED_COMMAND);
        }};
        dc.setEquipment(equipmentSet);
        device.setDeviceClass(dc);
        return device;
    }

    @Override
    public void run() throws HiveException, ExampleException, IOException {
        Device device = createDevice();
        try {
            hiveDevice.registerDevice(device);
            hiveDevice.authenticate(device.getId(), device.getKey());
            Device registered = hiveDevice.getDevice();
            print("Device registered! Device {}:", registered.getId());
            Timestamp serverTimestamp = hiveDevice.getInfo().getServerTimestamp();
            HiveMessageHandler<DeviceCommand> commandsHandler = new HiveMessageHandler<DeviceCommand>() {
                @Override
                public void handle(DeviceCommand command) {
                    if (command.getCommand().equals(LED_COMMAND)) {
                        JsonStringWrapper jsonString = command.getParameters();
                        JsonObject json = (JsonObject) new JsonParser().parse(jsonString.toString());
                        int state = json.get("state").getAsInt();
                        if (state == 0) {
                            view.setRed();
                            deviceState = false;
                        } else {
                            view.setGreen();
                            deviceState = true;
                        }
                        command.setStatus("Proceed");
                        command.setResult(new JsonStringWrapper("{status: \"Ok\"}"));
                        try {
                            hiveDevice.updateCommand(command);
                        } catch (HiveException e) {
                            if (state == 0) {
                                view.setGreen();
                                deviceState = true;
                            } else {
                                view.setRed();
                                deviceState = false;
                            }
                        }
                    }

                }
            };
            hiveDevice.subscribeForCommands(serverTimestamp, commandsHandler);
            Thread.currentThread().join(TimeUnit.MINUTES.toMillis(10));
        } catch (InterruptedException e) {
            throw new ExampleException(e.getMessage(), e);
        } finally {
            hiveDevice.close();
        }
    }
}
