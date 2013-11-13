package com.devicehive.client.example;


import com.devicehive.client.api.*;
import com.devicehive.client.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class AccessKeyRestExample {
    private static Logger logger = LoggerFactory.getLogger(AccessKeyRestExample.class);
    private Client client;

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        AccessKeyRestExample example = new AccessKeyRestExample();
        URI rest = URI.create(args[0]);
        URI websocket = URI.create(args[1]);
        try {
            example.init(rest, websocket);
            System.out.println("--- Device example ---");
            example.deviceExample(rest, websocket);
            System.out.println("--- Commands example ---");
            example.commandsExample(rest, websocket);
            System.out.println("--- Notifications example ---");
            example.notificationsExample(rest, websocket);
            System.out.println("--- Network example ---");
            example.networkExample(rest, websocket);
        } finally {
            example.close();
        }
    }

    private void close() {
        try {
            client.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void init(URI restUri, URI websocketUri) {
        client = new Client(restUri, websocketUri, Transport.REST_ONLY);
        client.authenticate("dhadmin", "dhadmin_#911");
    }

    public void deviceExample(URI rest, URI websocket) {
        try (Client deviceExampleClient = new Client(rest, websocket, Transport.REST_ONLY)) {
            String key = createAccessKey(AllowedAction.GET_DEVICE, AllowedAction.REGISTER_DEVICE,
                    AllowedAction.GET_DEVICE_STATE);
            deviceExampleClient.authenticate(key);
            DeviceController controller = deviceExampleClient.getDeviceController();
            //list
            List<Device> resultList = controller.listDevices(null, null, null, null, null, null, null, null,
                    "DeviceClass", null, null, null);
            for (Device currentDevice : resultList) {
                StringBuilder builder = new StringBuilder();
                builder.append("Id: ")
                        .append(currentDevice.getId())
                        .append("; device class id: ")
                        .append(currentDevice.getDeviceClass().getId());
                System.out.println(builder.toString());
            }
            //get
            Device existing = controller.getDevice(resultList.get(resultList.size() - 1).getId());
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ")
                    .append(existing.getId())
                    .append("; device class id: ")
                    .append(existing.getDeviceClass().getId());
            System.out.println(builder.toString());
            //register
            controller.registerDevice(existing.getId(), existing);
            //equipments
            List<DeviceEquipment> equipment = controller.getDeviceEquipment(existing.getId());
            for (DeviceEquipment currentEquipment : equipment) {
                StringBuilder equipmentsBuilder = new StringBuilder();
                builder.append("Id: ")
                        .append(currentEquipment.getId())
                        .append("; timestamp: ")
                        .append(currentEquipment.getTimestamp())
                        .append("; params: ")
                        .append(currentEquipment.getParameters().getJsonString());
                System.out.println(equipmentsBuilder.toString());
            }
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public void commandsExample(URI rest, URI websocket) {
        try (Client commandsExampleClient = new Client(rest, websocket, Transport.REST_ONLY)) {
            String key = createAccessKey(AllowedAction.GET_DEVICE_COMMAND, AllowedAction.UPDATE_DEVICE_COMMAND,
                    AllowedAction.CREATE_DEVICE_COMMAND);
            commandsExampleClient.authenticate(key);
            CommandsController controller = commandsExampleClient.getCommandsController();
            String guid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase();
            //query
            List<DeviceCommand> resultList = controller.queryCommands(guid, null, null, null, null, null, null, null,
                    null);
            for (DeviceCommand currentCommand : resultList) {
                StringBuilder builder = new StringBuilder().append("command id: ")
                        .append(currentCommand.getId())
                        .append("; command: ")
                        .append(currentCommand.getCommand());
                System.out.println(builder.toString());
            }
            //get
            DeviceCommand command = controller.getCommand(guid, resultList.get(resultList.size() - 1).getId());
            StringBuilder builder = new StringBuilder().append("command id: ")
                    .append(command.getId())
                    .append("; command: ")
                    .append(command.getCommand());
            System.out.println(builder.toString());
            //insert
            DeviceCommand newCommand = new DeviceCommand();
            newCommand.setCommand("example command");
            DeviceCommand inserted = controller.insertCommand(guid, command);
            System.out.println("Id: " + inserted.getId());
            //get commands updates queue
            controller.getCommandUpdatesQueue();
            //update
            inserted.setCommand(newCommand.getCommand());
            inserted.setStatus("proceed");
            controller.updateCommand(guid, inserted.getId(), inserted);
            //subscribe
            controller.subscribeForCommands(null, null, guid);
            //get commands subscription queue
            controller.getCommandQueue();
            //unsubscribe from commands
            controller.unsubscribeFromCommands(null, null, guid);
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public void notificationsExample(URI rest, URI websocket) {
        try (Client notificationsExampleClient = new Client(rest, websocket, Transport.REST_ONLY)) {
            String key =
                    createAccessKey(AllowedAction.GET_DEVICE_NOTIFICATION, AllowedAction.CREATE_DEVICE_NOTIFICATION);
            notificationsExampleClient.authenticate(key);
            NotificationsController controller = notificationsExampleClient.getNotificationsController();
            String guid = "E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase();
            //query
            List<DeviceNotification> resultList = controller.queryNotifications(guid, null, null, null, null, null,
                    null, null);
            for (DeviceNotification currentNotification : resultList) {
                StringBuilder builder = new StringBuilder().append("notification id: ")
                        .append(currentNotification.getId())
                        .append("; notification: ")
                        .append(currentNotification.getNotification());
                System.out.println(builder.toString());
            }
            //get
            DeviceNotification command =
                    controller.getNotification(guid, resultList.get(resultList.size() - 1).getId());
            StringBuilder builder = new StringBuilder().append("notification id: ")
                    .append(command.getId())
                    .append("; notification: ")
                    .append(command.getNotification());
            System.out.println(builder.toString());
            //insert
            DeviceNotification newNotification = new DeviceNotification();
            newNotification.setNotification("example notification");
            DeviceNotification inserted = controller.insertNotification(guid, command);
            System.out.println("Id: " + inserted.getId());
            //subscribe
            controller.subscribeForNotifications(null, null, guid);
            //get notifications subscription queue
            controller.getNotificationsQueue();
            //unsubscribe from notifications
            controller.unsubscribeFromNotification(null, null, guid);
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public void networkExample(URI rest, URI websocket) {
        try (Client networkExampleClient = new Client(rest, websocket, Transport.REST_ONLY)) {
            String key =
                    createAccessKey(AllowedAction.GET_NETWORK);
            networkExampleClient.authenticate(key);
            NetworkController controller = networkExampleClient.getNetworkController();
            //list
            List<Network> resultList = controller.listNetworks(null, null, null, null, null, null);
            for (Network currentNetwork : resultList) {
                StringBuilder builder = new StringBuilder().append("Id: ")
                        .append(currentNetwork.getId())
                        .append("; name: ")
                        .append(currentNetwork.getName())
                        .append("; description: ")
                        .append(currentNetwork.getDescription());
                System.out.println(builder.toString());
            }
            //get
            Network network = controller.getNetwork(resultList.get(resultList.size() - 1).getId());
            StringBuilder builder = new StringBuilder().append("Id: ")
                    .append(network.getId())
                    .append("; name: ")
                    .append(network.getName())
                    .append("; description: ")
                    .append(network.getDescription());
            System.out.println(builder.toString());
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    private String createAccessKey(AllowedAction... actionsToInsert) {
        AccessKey toInsert = new AccessKey();
        toInsert.setLabel("example");
        AccessKeyPermission permission = new AccessKeyPermission();
        Set<String> actions = new HashSet<>();
        for (AllowedAction action : actionsToInsert) {
            actions.add(action.getValue());
        }
        permission.setActions(actions);
        Set<AccessKeyPermission> permissionSet = new HashSet<>();
        permissionSet.add(permission);
        toInsert.setPermissions(permissionSet);
        Long userId = 1L;
        return client.getAccessKeyController().insertKey(userId, toInsert).getKey();
    }

}
