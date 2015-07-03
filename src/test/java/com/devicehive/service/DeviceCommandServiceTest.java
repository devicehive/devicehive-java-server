package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.User;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeviceCommandServiceTest extends AbstractResourceTest {
    private static final String DEFAULT_STATUS = "default_status";

    @Autowired
    private DeviceCommandService deviceCommandService;

    /**
     * Simple test to check that all command were successfully saved and than retrieved
     */
    @Test
    public void testSubmitDeviceCommands() {
        final int NUMBER_OF_COMMANDS = 100;
        sendNCommands(NUMBER_OF_COMMANDS);

        final Collection<DeviceCommand> commands =  deviceCommandService.find(
                null, Collections.<String>emptyList(), null, DEFAULT_STATUS, 100, true, null);
        assertEquals(NUMBER_OF_COMMANDS, commands.size());
    }

    /**
     * Tests all commands were saved with increasing timestamp
     */
    @Test
    public void testIncreasingTimestamps() {
        final int NUMBER_OF_COMMANDS = 20;
        sendNCommands(NUMBER_OF_COMMANDS);

        final List<DeviceCommand> commands =  new ArrayList<DeviceCommand>(deviceCommandService.find(
                null, Collections.<String>emptyList(), null, DEFAULT_STATUS, 100, true, null));

        for (int i = 1; i < commands.size(); i++) {
            final Date currentElem = commands.get(i).getTimestamp();
            final Date previousElem = commands.get(i - 1).getTimestamp();
            assertTrue(currentElem.before(previousElem));
        }
    }

    @Test
    public void _testIncreasingTimestamps() {
        final int NUMBER_OF_COMMANDS = 20;
        sendNCommands(NUMBER_OF_COMMANDS);

        final List<DeviceCommand> commands =  new ArrayList<DeviceCommand>(deviceCommandService.find(
                null, Collections.<String>emptyList(), null, DEFAULT_STATUS, 100, true, null));

        for (int i = 1; i < commands.size(); i++) {
            final Date currentElem = commands.get(i).getTimestamp();
            final Date previousElem = commands.get(i - 1).getTimestamp();
            assertTrue(currentElem.before(previousElem));
        }
    }

    /**
     * Tests ability to retrieve commands which were after specified date
     */
    @Test
    public void testRetrieveCommandsFromDate() {
        final Timestamp timeBeforeBatches = new Timestamp(new Date().getTime());
        sendNCommands(10);
        try {
            Thread.sleep(1000); //need it to have delay between two groups of commands
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Timestamp timeBetweenBatches = new Timestamp(new Date().getTime());
        sendNCommands(15);

        final Collection<DeviceCommand> commandsAll =  deviceCommandService.find(
                null, Collections.<String>emptyList(), timeBeforeBatches, DEFAULT_STATUS, 100, true, null);
        assertEquals(25, commandsAll.size());


        final Collection<DeviceCommand> commands =  deviceCommandService.find(
                null, Collections.<String>emptyList(), timeBetweenBatches, DEFAULT_STATUS, 100, true, null);
        assertEquals(15, commands.size());

    }

    /**
     * Tests ability to retrieve commands by name
     */
    @Test
    public void testRetrieveCommandsByName() {
        sendNCommands(10);

        final Collection<DeviceCommand> commands =  deviceCommandService.find(
                null, Arrays.asList("command2", "command3", "command4"), null, DEFAULT_STATUS, 100, true, null);
        assertEquals(3, commands.size());
    }

    private void sendNCommands(int n, String status) {
        for (int i = 0; i < n; i++) {
            //Need this hack to have different timestamp for each command
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            final DeviceCommand deviceCommand = deviceCommandService.convertToDeviceCommand(
                    new DeviceCommandWrapper(),
                    new Device(),
                    new User(),
                    0L);

            deviceCommand.setUserId(0L);
            deviceCommand.setDeviceGuid(UUID.randomUUID().toString());
            deviceCommand.setCommand("command"+i);
            deviceCommand.setParameters(new JsonStringWrapper("{'test':'test'}"));
            deviceCommand.setStatus(status);

            deviceCommandService.store(deviceCommand);
        }
    }

    private void sendNCommands(int n) {
        sendNCommands(n, DEFAULT_STATUS);
    }
}