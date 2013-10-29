package com.devicehive.client.api;


import com.devicehive.client.config.Constants;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class SingleHiveDevice implements Closeable {

    private HiveContext hiveContext;
    private ScheduledExecutorService subscriptionExecutor = Executors.newSingleThreadScheduledExecutor();

    public SingleHiveDevice(URI restUri) {
        this.hiveContext = new HiveContext(Transport.AUTO, restUri);
    }

    public static void main(String... args) {
        SingleHiveDevice shd = new SingleHiveDevice(URI.create("http://127.0.0.1:8080/hive/rest/"));
        shd.authenticate("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8");
//        Device device = shd.getDevice();
//        Gson gson = GsonFactory.createGson();
//        JsonObject obj = (JsonObject) gson.toJsonTree(device);
//        System.out.println(obj.toString());
//        device.setName("changedName");
//        shd.saveDevice(device);
//        List<DeviceCommand> commands = shd.queryCommands(null, null, null, null, "Command", true, null, null);
//        System.out.println(commands.toString());
//        DeviceCommand command = shd.getCommand(1L);
//        System.out.print(command.toString());
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            shd.subscribeForCommands(new Timestamp(startDate.getTime()), 40);
        } catch (ParseException e) {
            System.out.print(e);
        }
    }

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    public void authenticate(String deviceId, String deviceKey) {
        hiveContext.setHivePrincipal(HivePrincipal.createDevice(deviceId, deviceKey));
    }

    public Device getDevice() {
        String deviceId = hiveContext.getHivePrincipal().getDevice().getKey();
        if (deviceId == null) {
            throw new HiveClientException("Device is not authenticated");
        }
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, Device.class, null);
    }

    public void saveDevice(Device device) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        device.setKey("05F94BF509C8");
        HiveValidator.validate(device);
        String path = "/device/" + device.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, device, null);
    }

    public List<DeviceCommand> queryCommands(Timestamp start, Timestamp end, String command, String status,
                                             String sortBy, boolean sortAsc, Integer take, Integer skip) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("command", command);
        queryParams.put("status", status);
        queryParams.put("sortField", sortBy);
        String order = sortAsc ? "ASC" : "DESC";
        queryParams.put("sortOrder", order);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), null);
    }

    public DeviceCommand getCommand(long commandId) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, DeviceCommand.class, null);
    }

    public void updateCommand(DeviceCommand deviceCommand) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + deviceCommand.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, deviceCommand, COMMAND_UPDATE_FROM_DEVICE);
    }

    public void subscribeForCommands(final Timestamp timestamp, final Integer waitTimeout) {
        if (!subscriptionExecutor.isShutdown() && !subscriptionExecutor.isTerminated()) {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            final BlockingQueue<DeviceCommand> commandQueue = hiveContext.getCommandQueue();
            final String path = "/device/" + authenticated.getKey() + "/command/poll";
            subscriptionExecutor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    Map<String, Object> queryParams = new HashMap<>();
                    queryParams.put("timestamp", timestamp);
                    queryParams.put("waitTimeout", waitTimeout);
                    try {
                        List<DeviceCommand> returned =
                                hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET,
                                        queryParams, null, new TypeToken<List<DeviceCommand>>() {
                                }.getType(), null, COMMAND_LISTED);
                        System.out.println("\n----Start Timestamp: " + timestamp + "----");
                        for (DeviceCommand current : returned) {
                            System.out.println("id: " + current.getId() + "timestamp:" + current.getTimestamp());
                        }
                        if (!returned.isEmpty()) {
                            commandQueue.addAll(returned);
                            timestamp.setTime(returned.get(returned.size() - 1).getTimestamp().getTime());
                        } else {
                            //todo set timestamp. do not try to get extra commands.
                        }

                    } catch (HiveException e) {
                        System.err.println(e);
                    }
                }
            }, 0, Constants.DELAY, TimeUnit.SECONDS);
        }
    }

    public void unsubscribeFromCommands() {
        subscriptionExecutor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!subscriptionExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                subscriptionExecutor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!subscriptionExecutor.awaitTermination(60, TimeUnit.SECONDS))
                    throw new InternalHiveClientException("Unable to unsubscribe from commands! subscriptionExecutor " +
                            "did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            subscriptionExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        subscriptionExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public DeviceNotification insertNotification(DeviceNotification deviceNotification) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        HiveValidator.validate(deviceNotification);
        String path = "/device/" + authenticated.getKey() + "notification";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, deviceNotification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
    }

}
