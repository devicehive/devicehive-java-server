package com.devicehive.test;

import com.devicehive.client.*;
import com.devicehive.client.impl.HiveClientRestImpl;
import com.devicehive.client.impl.HiveDeviceRestImpl;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.exceptions.HiveException;
import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RunWith(JUnit4.class)
public class PollingTest {

    private final Lock lock = new ReentrantLock();
    private ScheduledExecutorService commandsInsertService = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean greenState = false;
    private volatile boolean redState = false;
    private volatile int i = 0;
    private ScheduledExecutorService commandsUpdatesService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService commandsUpdatesProcessor = Executors.newSingleThreadScheduledExecutor();
    private HiveClient client;
    private HiveDevice shd;

    @Test
    public void commandsPollingTest() {
        try {
            shd = new HiveDeviceRestImpl(URI.create("http://jk-pc:8080/DeviceHiveJava/rest/"),
                    Transport.PREFER_WEBSOCKET);
            shd.authenticate("E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase(), "05F94BF509C8");
            client = new HiveClientRestImpl(URI.create("http://jk-pc:8080/DeviceHiveJava/rest/"), Transport.PREFER_WEBSOCKET);
            client.authenticate("dhadmin", "dhadmin_#911");
            final CommandsController controller = client.getCommandsController();
            final DeviceCommand command = new DeviceCommand();
            command.setCommand("UpdateLedState");
            commandsInsertService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        lock.lock();
                        try {
                            if (i % 2 == 0) {
                                if (greenState) {
                                    command.setParameters(
                                            new JsonStringWrapper("{\"equipment\":\"LED_G\",\"state\":1}"));
                                    greenState = !greenState;
                                } else {
                                    command.setParameters(
                                            new JsonStringWrapper("{\"equipment\":\"LED_G\",\"state\":0}"));
                                    greenState = !greenState;
                                }
                            } else {
                                if (redState) {
                                    command.setParameters(
                                            new JsonStringWrapper("{\"equipment\":\"LED_R\",\"state\":1}"));
                                    redState = !redState;
                                } else {
                                    command.setParameters(
                                            new JsonStringWrapper("{\"equipment\":\"LED_R\",\"state\":0}"));
                                    redState = !redState;
                                }
                            }
                            i++;
                            System.out.println("Command inserted. Command id: " +
                                    controller.insertCommand("E50D6085-2ABA-48E9-B1C3-73C673E414BE".toLowerCase(),
                                            command).getId());
                        } finally {
                            lock.unlock();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);
            shd.subscribeForCommands(null);
            commandUpdateServiceStart();
            commandUpdatesProcessorStart();
            Thread.currentThread().join(360_000);
        } catch (Exception e) {
            e.printStackTrace();
            if (!(e instanceof HiveException))
                TestCase.fail("No exception expected: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void commandUpdateServiceStart() {
        final CommandsController controller = client.getCommandsController();
        commandsUpdatesService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Queue<DeviceCommand> queue = controller.getCommandUpdatesQueue();
                while (!queue.isEmpty()) {
                    DeviceCommand command = queue.poll();
                    System.out.println("command updated: " + command.getId());
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void commandUpdatesProcessorStart() {
        commandsUpdatesProcessor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("Try to update commands");
                Queue<Pair<String, DeviceCommand>> queue = shd.getCommandsQueue();
                while (!queue.isEmpty()) {
                    Pair<String, DeviceCommand> commandAssociation = queue.poll();
                    DeviceCommand command = commandAssociation.getRight();
                    command.setStatus("Status");
                    command.setResult(new JsonStringWrapper("{\"ololo\":\"result\"}"));
                    shd.updateCommand(command);
                    System.out.println("Command update sent. Command id: " + command.getId());
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void close() {
        try {
            if (client != null)
                client.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            commandsInsertService.shutdown();
            commandsUpdatesService.shutdown();
            commandsUpdatesProcessor.shutdown();
        }
    }
}
