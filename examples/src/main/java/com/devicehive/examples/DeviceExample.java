package com.devicehive.examples;


import com.devicehive.client.HiveDevice;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceClass;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Equipment;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.google.gson.JsonObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.devicehive.constants.Constants.USE_SOCKETS;

public class DeviceExample extends Example {
    private static final String ID = "id";
    private static final String ID_DESCRIPTION = "Device unique identifier.";
    private static final String KEY = "key";
    private static final String KEY_DESCRIPTION = "Device authentication key";
    private static final String NAME = "name";
    private static final String NAME_DESCRIPTION = "Device name";
    private static final String STATUS = "status";
    private static final String STATUS_DESCRIPTION = "Device operation status";
    private static final String NETWORK = "hasNetwork";
    private static final String NETWORK_DESCRIPTION = "An option that indicates if the device should have an network" +
            ". Default is false. In case of true the network with the random data will be created";
    private static final String DC_NAME = "dcName";
    private static final String DC_NAME_DESCRIPTION = "Device class name.";
    private static final String DC_VERSION = "dcVersion";
    private static final String DC_VERSION_DESCRIPTION = "Device class version.";
    private static final String DC_PERMANENT = "isPermanent";
    private static final String DC_PERMANENT_DESCRIPTION = "Indicates whether device class is permanent. Permanent " +
            "device classes could not be modified by devices during registration.";
    private static final String DC_OFFLINE_TIMEOUT = "dcTimeout";
    private static final String DC_OFFLINE_TIMEOUT_DESCRIPTION = "If set, specifies inactivity timeout in seconds " +
            "before the framework changes device status to 'Offline'.";
    private final HiveDevice hiveDevice;
    private final CommandLine commandLine;

    public DeviceExample(PrintStream err, PrintStream out, String... args) throws HiveException, ExampleException {
        super(out, args);
        commandLine = getCommandLine();
        hiveDevice = HiveFactory.createDevice(getServerUrl(),
                commandLine.hasOption(USE_SOCKETS),
                Example.HIVE_CONNECTION_EVENT_HANDLER);
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
        Options options = new Options();
        Option id = new Option(ID, true, ID_DESCRIPTION);
        id.setRequired(true);
        options.addOption(id);
        Option keyOpt = new Option(KEY, true, KEY_DESCRIPTION);
        keyOpt.setRequired(true);
        options.addOption(keyOpt);
        Option nameOption = new Option(NAME, true, NAME_DESCRIPTION);
        nameOption.setRequired(true);
        options.addOption(nameOption);
        options.addOption(STATUS, true, STATUS_DESCRIPTION);
        options.addOption(NETWORK, false, NETWORK_DESCRIPTION);
        Option dcName = new Option(DC_NAME, true, DC_NAME_DESCRIPTION);
        dcName.setRequired(true);
        options.addOption(dcName);
        Option dcVersion = new Option(DC_VERSION, true, DC_VERSION_DESCRIPTION);
        dcVersion.setRequired(true);
        options.addOption(dcVersion);
        options.addOption(DC_PERMANENT, false, DC_PERMANENT_DESCRIPTION);
        options.addOption(DC_OFFLINE_TIMEOUT, true, DC_OFFLINE_TIMEOUT_DESCRIPTION);
        return options;
    }

    private Device createDevice() throws ExampleException {
        Device device = new Device();
        device.setId(commandLine.getOptionValue(ID));
        device.setKey(commandLine.getOptionValue(KEY));
        device.setName(commandLine.getOptionValue(NAME));
        if (commandLine.hasOption(STATUS))
            device.setStatus(commandLine.getOptionValue(STATUS));
        JsonObject dataJson = new JsonObject();
        dataJson.addProperty("data", "some_example_data");
        device.setData(new JsonStringWrapper(dataJson.toString()));
        if (commandLine.hasOption(NETWORK))
            //todo create network
            ;
        DeviceClass dc = new DeviceClass();
        dc.setName(commandLine.getOptionValue(DC_NAME));
        dc.setVersion(commandLine.getOptionValue(DC_VERSION));
        if (commandLine.hasOption(DC_PERMANENT))
            dc.setPermanent(true);
        else dc.setPermanent(false);
        if (commandLine.hasOption(DC_OFFLINE_TIMEOUT))
            try {
                dc.setOfflineTimeout(Integer.parseInt(commandLine.getOptionValue(DC_OFFLINE_TIMEOUT)));
            } catch (NumberFormatException e) {
                throw new ExampleException("Unable to parse offline timeout value!", e);
            }
        Set<Equipment> dummyEquipmentSet = new HashSet<Equipment>() {{
            Equipment dummyEquipment = new Equipment();
            dummyEquipment.setName("example_equipment_name");
            dummyEquipment.setType("example_equipment_type");
            dummyEquipment.setCode(UUID.randomUUID().toString());
            add(dummyEquipment);
        }};
        dc.setEquipment(dummyEquipmentSet);
        device.setDeviceClass(dc);
        return device;
    }

    private DeviceNotification createNotification() {
        DeviceNotification dn = new DeviceNotification();
        dn.setNotification("new state");
        JsonObject parametersJson = new JsonObject();
        parametersJson.addProperty("time", new Date().toString());
        parametersJson.addProperty("example", "there can be every info you would like to see");
        dn.setParameters(new JsonStringWrapper(parametersJson.toString()));
        return dn;
    }

    @Override
    public void run() throws HiveException, ExampleException, IOException {
        Device device = createDevice();
        try {
            hiveDevice.registerDevice(device);
            hiveDevice.authenticate(device.getId(), device.getKey());
            Device registered = hiveDevice.getDevice();
            print("Device registered! Device {}:", registered);
            Timestamp serverTimestamp = hiveDevice.getInfo().getServerTimestamp();
            HiveMessageHandler<DeviceCommand> commandsHandler = new HiveMessageHandler<DeviceCommand>() {
                @Override
                public void handle(DeviceCommand command) {
                    print("Command received: {}", command.getCommand());
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

    public final void createNetwork() {

    }
}
