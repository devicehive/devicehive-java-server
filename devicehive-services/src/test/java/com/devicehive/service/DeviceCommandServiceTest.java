package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeviceCommandServiceTest extends AbstractResourceTest {
    private static final String DEFAULT_STATUS = "default_status";
    private static final Boolean DEFAULT_IS_UPDATED = false;

    @Autowired
    private DeviceCommandService deviceCommandService;

    @Test
    public void testFindAllCommands(){
        final int NUMBER_OF_COMMANDS = 99;

        for(int i = 0; i < NUMBER_OF_COMMANDS; i++){
                sendNCommands(1, DEFAULT_STATUS, i % 2 == 0);
        }

        assertEquals(NUMBER_OF_COMMANDS, deviceCommandService.find(
                null, Collections.emptyList(), null, DEFAULT_STATUS, 100, null).join().size());
    }

    @Test
    public void testFindCommandsWithResponse(){
        final int NUMBER_OF_COMMANDS = 99;

        int withResponse = 0;

        for(int i = 0; i < NUMBER_OF_COMMANDS; i++){
            if(i % 2 == 0) {
                withResponse++;
                sendNCommands(1, DEFAULT_STATUS, true);
            }else{
                sendNCommands(1, DEFAULT_STATUS, false);
            }
        }
        assertEquals(withResponse, deviceCommandService.find(
                null, Collections.emptyList(), null, DEFAULT_STATUS, 100, true).join().size()
                );
    }
    @Test
    public void testFindCommandsWithoutResponse(){
        final int NUMBER_OF_COMMANDS = 99;

        int withResponse = 0;

        for(int i = 0; i < NUMBER_OF_COMMANDS; i++){
            if(i % 2 == 0) {
                withResponse++;
                sendNCommands(1, DEFAULT_STATUS, true);
            }else{
                sendNCommands(1, DEFAULT_STATUS, false);
            }
        }
        assertEquals(NUMBER_OF_COMMANDS - withResponse, deviceCommandService.find(
                null, Collections.emptyList(), null, DEFAULT_STATUS, 100, false).join().size()
                );
    }


    /**
     * Simple test to check that all command were successfully saved and than retrieved
     */
    @Test
    public void testSubmitDeviceCommands() {
        final int NUMBER_OF_COMMANDS = 100;
        sendNCommands(NUMBER_OF_COMMANDS);

        final Collection<DeviceCommand> commands =  deviceCommandService.find(
                null, Collections.emptyList(), null, DEFAULT_STATUS, 100, false).join();
        assertEquals(NUMBER_OF_COMMANDS, commands.size());
    }

    /**
     * Tests all commands were saved with increasing timestamp
     */
    @Test
    public void testIncreasingTimestamps() {
        final int NUMBER_OF_COMMANDS = 20;
        sendNCommands(NUMBER_OF_COMMANDS);

        final List<DeviceCommand> commands = new ArrayList<>(deviceCommandService.find(
                null, Collections.emptyList(), null, DEFAULT_STATUS, 100, false).join());

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
                null, Collections.emptyList(), timeBeforeBatches, DEFAULT_STATUS, 100, false).join();
        assertEquals(25, commandsAll.size());


        final Collection<DeviceCommand> commands =  deviceCommandService.find(
                null, Collections.emptyList(), timeBetweenBatches, DEFAULT_STATUS, 100, false).join();
        assertEquals(15, commands.size());

    }

    /**
     * Tests ability to retrieve commands by name
     */
    @Test
    public void testRetrieveCommandsByName() {
        sendNCommands(10);

        final Collection<DeviceCommand> commands =  deviceCommandService.find(
                null, Arrays.asList("command2", "command3", "command4"), null, DEFAULT_STATUS, 100, false).join();
        assertEquals(3, commands.size());
    }

    private void sendNCommands(int n, String status, boolean isUpdated) {
        for (int i = 0; i < n; i++) {
            //Need this hack to have different timestamp for each command
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            final DeviceCommand deviceCommand = new DeviceCommand();

            deviceCommand.setId(Math.abs(new Random().nextInt()));
            deviceCommand.setTimestamp(new Date());
            deviceCommand.setUserId(0L);
            deviceCommand.setDeviceGuid(UUID.randomUUID().toString());
            deviceCommand.setCommand("command"+i);
            deviceCommand.setParameters(new JsonStringWrapper("{'test':'test'}"));
            deviceCommand.setStatus(status);
            deviceCommand.setIsUpdated(isUpdated);

            deviceCommandService.store(deviceCommand);
        }
    }

    private void sendNCommands(int n) {
        sendNCommands(n, DEFAULT_STATUS, DEFAULT_IS_UPDATED);
    }
}