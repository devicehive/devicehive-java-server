package com.devicehive.client.example.user;


import com.devicehive.client.*;
import com.devicehive.client.impl.HiveClientImpl;
import com.devicehive.client.model.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserExample {

    private static Logger logger = LoggerFactory.getLogger(UserExample.class);
    private final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private HiveClientImpl client;
    private PrintStream out;
    private Options options = new Options();
    private URI rest;
    private boolean isParseable = true;
    private Transport transport;

    protected UserExample(PrintStream out) {
        this.out = out;
    }

    public static void main(String... args) {
        UserExample example = new UserExample(System.out);
        example.run(args);
    }

    public void run(String... args) {
        initOptions();
        parseArguments(args);
        if (isParseable)
            try {
                init();
                out.println("--- User example ---");
                userExample();
                out.println("--- Network example ---");
                networkExample();
                out.println("--- Device notification example ---");
                deviceNotificationExample();
                out.println("--- Device command example ---");
                deviceCommandExample();
                out.println("--- Device example ---");
                deviceExample();
                out.println("--- Access key example ---");
                accessKeysExample();
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            } finally {
                close();
            }
    }

    private void init() {
        client = new HiveClientImpl(rest, transport);
        client.authenticate("dhadmin", "dhadmin_#911");
    }

    private void close() {
        try {
            client.close();
        } catch (IOException e) {
            logger.warn("unable to close client", e);
        }
    }

    private void accessKeysExample() {
        AccessKeyController controller = client.getAccessKeyController();
        Long dhadminId = 1L;
        //list
        List<AccessKey> resultList = controller.listKeys(dhadminId);
        for (AccessKey currentKey : resultList) {
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(currentKey.getId())
                    .append("; label: ")
                    .append(currentKey.getLabel())
                    .append("; key: ")
                    .append(currentKey.getKey())
                    .append("; expiration date: ")
                    .append(currentKey.getExpirationDate());
            out.println(builder.toString());
        }
        //insert
        AccessKey toInsert = new AccessKey();
        toInsert.setLabel("example");
        AccessKeyPermission permission = new AccessKeyPermission();
        Set<String> actions = new HashSet<>();
        actions.add(AllowedAction.CREATE_DEVICE_COMMAND.getValue());
        actions.add(AllowedAction.GET_DEVICE_NOTIFICATION.getValue());
        permission.setActions(actions);
        Set<AccessKeyPermission> permissionSet = new HashSet<>();
        permissionSet.add(permission);
        toInsert.setPermissions(permissionSet);
        AccessKey inserted = controller.insertKey(dhadminId, toInsert);
        toInsert.setId(inserted.getId());
        toInsert.setKey(inserted.getKey());
        //update
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2020-10-11 13:12:00");
            toInsert.setExpirationDate(new Timestamp(startDate.getTime()));
            controller.updateKey(dhadminId, inserted.getId(), toInsert);
        } catch (ParseException e) {
            logger.warn(e.getMessage(), e);
        }
        //get
        AccessKey existing = controller.getKey(dhadminId, inserted.getId());
        StringBuilder builder = new StringBuilder();
        builder.append("Id: ")
                .append(existing.getId())
                .append("; label: ")
                .append(existing.getLabel())
                .append("; key: ")
                .append(existing.getKey())
                .append("; expiration date: ")
                .append(existing.getExpirationDate());
        out.println(builder.toString());
        //delete
        controller.deleteKey(dhadminId, existing.getId());
    }

    private void deviceExample() {
        DeviceController controller = client.getDeviceController();
        //register
        Device device = createDeviceToSave();
        controller.registerDevice(device.getId(), device);
        //list devices
        List<Device> devices = controller.listDevices(null, null, null, null, null, null, null, null, null, null,
                null, null);
        for (Device currentDevice : devices) {
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(currentDevice.getId())
                    .append("; device class id: ")
                    .append(currentDevice.getDeviceClass().getId());
            out.println(builder.toString());
        }
        //get equipment list
        List<DeviceEquipment> equipment = controller.getDeviceEquipment(device.getId());
        for (DeviceEquipment currentEquipment : equipment) {
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(currentEquipment.getId())
                    .append("; timestamp: ")
                    .append(currentEquipment.getTimestamp())
                    .append("; params: ")
                    .append(currentEquipment.getParameters().getJsonString());
            out.println(builder.toString());
        }
        //list device class
        List<DeviceClass> resultList = controller.listDeviceClass(null, null, null, "Name", "ASC", null, null);
        for (DeviceClass currentClass : resultList) {
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(currentClass.getId())
                    .append("; name: ")
                    .append(currentClass.getName())
                    .append("; version: ")
                    .append(currentClass.getVersion())
                    .append("; data: ")
                    .append(currentClass.getData().getJsonString());
            out.println(builder.toString());
        }
        //insert device class
        DeviceClass toInsert = new DeviceClass();
        toInsert.setName("example name");
        toInsert.setVersion("example version");
        toInsert.setId(controller.insertDeviceClass(toInsert));
        //update device class
        toInsert.setData(new JsonStringWrapper("{\"data\": \"new data\"}"));
        controller.updateDeviceClass(toInsert.getId(), toInsert);
        //get device class
        DeviceClass existing = controller.getDeviceClass(toInsert.getId());
        StringBuilder builder = new StringBuilder();
        builder.append("Id: ")
                .append(existing.getId())
                .append("; name: ")
                .append(existing.getName())
                .append("; version: ")
                .append(existing.getVersion())
                .append("; data: ")
                .append(existing.getData().getJsonString());
        out.println(builder.toString());
        //delete device class
        controller.deleteDeviceClass(existing.getId());
        //release resources
        Device created = controller.getDevice(device.getId());
        controller.deleteDeviceClass(created.getDeviceClass().getId());
        //delete device
        controller.deleteDevice(device.getId());

    }

    private Device createDeviceToSave() {
        Device device = new Device();
        device.setId(UUID.randomUUID().toString());
        device.setKey(UUID.randomUUID().toString());
        device.setStatus("new device example status");
        device.setName("example device");
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setName("example device class name");
        deviceClass.setVersion("v1");
        deviceClass.setPermanent(true);
        Set<Equipment> equipmentSet = new HashSet<>();
        Equipment equipment = new Equipment();
        equipment.setName("example equipment name");
        equipment.setCode(UUID.randomUUID().toString());
        equipment.setType("example");
        equipmentSet.add(equipment);
        deviceClass.setEquipment(equipmentSet);
        device.setDeviceClass(deviceClass);
        Network network = new Network();
        network.setId(1L);
        network.setName("VirtualLed Sample Network");
        device.setNetwork(network);
        return device;
    }

    private void deviceCommandExample() {
        CommandsController controller = client.getCommandsController();
        String guid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase();
        //insert command
        DeviceCommand command = new DeviceCommand();
        command.setStatus("new");
        command.setCommand("example");
        DeviceCommand inserted = controller.insertCommand(guid, command);
        //get command updates queue
        Queue<DeviceCommand> commandUpdatesQueue = controller.getCommandUpdatesQueue();
        //check if command proceed
        boolean isProceed = false;
        for (DeviceCommand currentCommand : commandUpdatesQueue) {
            if (currentCommand.getId().equals(inserted.getId())) {
                isProceed = true;
                break;
            }
        }
        out.println("Is command proceed: " + isProceed);
    }

    private void deviceNotificationExample() {
        NotificationsController controller = client.getNotificationsController();
        //query
        String guid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase();
        Long notificationId = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            List<DeviceNotification> resultList =
                    controller.queryNotifications(guid, new Timestamp(startDate.getTime()),
                            null, null, null, null, null, 5, null);
            for (DeviceNotification currentNotification : resultList) {
                StringBuilder builder = new StringBuilder();
                builder.append("Id: ")
                        .append(currentNotification.getId())
                        .append("; timestamp: ")
                        .append(currentNotification.getTimestamp())
                        .append("; notification: ")
                        .append(currentNotification.getNotification())
                        .append("; params: ")
                        .append(currentNotification.getParameters().getJsonString());
                out.println(builder.toString());
                notificationId = currentNotification.getId();
            }
        } catch (ParseException e) {
            logger.debug(e.getMessage(), e);
        }
        //get
        if (notificationId != null) {
            DeviceNotification notification = controller.getNotification(guid, notificationId);
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(notification.getId())
                    .append("; timestamp: ")
                    .append(notification.getTimestamp())
                    .append("; notification: ")
                    .append(notification.getNotification())
                    .append("; params: ")
                    .append(notification.getParameters().getJsonString());
            out.println(builder.toString());
        }
        //subscribe
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            controller.subscribeForNotifications(new Timestamp(startDate.getTime()), null, guid);
            logger.debug("device subscribed for commands");
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        //unsubscribe
        try {
            Thread.currentThread().join(10_000);
        } catch (InterruptedException e) {
            logger.info(e.getMessage(), e);
        }
        controller.unsubscribeFromNotification(null, guid);
        //get notifications queue
        Queue<Pair<String, DeviceNotification>> notificationsQueue = controller.getNotificationsQueue();
    }

    private void networkExample() {
        NetworkController controller = client.getNetworkController();
        //list
        List<Network> resultList = controller.listNetworks(null, null, "ID", "ASC", null, 1);
        for (Network currentNetwork : resultList) {
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(currentNetwork.getId())
                    .append("; key: ")
                    .append(currentNetwork.getKey())
                    .append("; name: ")
                    .append(currentNetwork.getName())
                    .append("; description: ")
                    .append(currentNetwork.getDescription());
            out.println(builder.toString());
        }
        //insert
        Network networkToInsert = new Network();
        networkToInsert.setName("example_name");
        networkToInsert.setKey("example_key");
        networkToInsert.setDescription("example_description");
        networkToInsert.setId(controller.insertNetwork(networkToInsert));
        //get
        Network existing = controller.getNetwork(networkToInsert.getId());
        StringBuilder builder = new StringBuilder();
        builder.append("Id: ")
                .append(existing.getId())
                .append("; key: ")
                .append(existing.getKey())
                .append("; name: ")
                .append(existing.getName())
                .append("; description: ")
                .append(existing.getDescription());
        out.println(builder.toString());
        //update
        existing.setDescription("new example description");
        controller.updateNetwork(existing.getId(), existing);
        //delete
        controller.deleteNetwork(existing.getId());
    }

    private void userExample() {
        UserController controller = client.getUserController();
        //list
        List<User> resultList = controller.listUsers(null, "%in", null, null, null, "ASC", 100, null);
        for (User currentUser : resultList) {
            out.println("Id: " + currentUser.getId() + "; login: " + currentUser.getLogin() + "; role: " +
                    currentUser.getRole() + "; status: " + currentUser.getStatus());
        }
        //get
        User current = controller.getUser();
        out.println("Id: " + current.getId() + "; login: " + current.getLogin() + "; role: " +
                current.getRole() + "; status: " + current.getStatus() + "; last login: " + current.getLastLogin());
        if (current.getNetworks() != null) {
            for (UserNetwork currentNetwork : current.getNetworks()) {
                StringBuilder builder = new StringBuilder();
                builder.append("Id: ")
                        .append(currentNetwork.getNetwork().getId())
                        .append("; key: ")
                        .append(currentNetwork.getNetwork().getKey())
                        .append("; name: ")
                        .append(currentNetwork.getNetwork().getName())
                        .append("; description: ")
                        .append(currentNetwork.getNetwork().getDescription());
                out.println(builder.toString());
            }
        }
        //insert
        User toInsert = new User();
        toInsert.setLogin("user_" + System.currentTimeMillis());
        toInsert.setRole(1);
        toInsert.setStatus(0);
        toInsert.setPassword("example");
        User inserted = controller.insertUser(toInsert);
        toInsert.setId(inserted.getId());
        toInsert.setLastLogin(inserted.getLastLogin());
        //update
        toInsert.setPassword("update_example");
        controller.updateUser(toInsert.getId(), toInsert);
        Long networkId = 1l;
        //assign network
        controller.assignNetwork(toInsert.getId(), networkId);
        //get network
        UserNetwork assignedNetwork = controller.getNetwork(toInsert.getId(), networkId);
        out.println("Name: " + assignedNetwork.getNetwork().getName() + "; key: " + assignedNetwork.getNetwork
                ().getKey());
        //unassign network
        controller.unassignNetwork(toInsert.getId(), networkId);
        //delete
        controller.deleteUser(toInsert.getId());
    }

    private void printUsage() {
        HELP_FORMATTER.printHelp("SingleHiveDeviceExample", options);
    }

    private void parseArguments(String... args) {
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmdLine = parser.parse(options, args);
            rest = URI.create(cmdLine.getOptionValue("rest"));
            transport = cmdLine.hasOption("use_sockets") ? Transport.PREFER_WEBSOCKET : Transport.REST_ONLY;
        } catch (org.apache.commons.cli.ParseException e) {
            logger.error("unable to parse command line arguments!");
            printUsage();
            isParseable = false;
        }
    }

    private void initOptions() {
        Option restUrl = OptionBuilder.hasArg()
                .withArgName("rest")
                .withDescription("REST service URL")
                .isRequired(true)
                .create("rest");
        Option transport = OptionBuilder.hasArg(false)
                .withDescription("if set use sockets")
                .create("use_sockets");
        options.addOption(restUrl);
        options.addOption(transport);
    }

}
