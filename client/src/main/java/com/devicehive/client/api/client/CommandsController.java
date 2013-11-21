package com.devicehive.client.api.client;


import com.devicehive.client.model.DeviceCommand;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Timestamp;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Client side controller for device commands: <i>/device/{deviceGuid}/command</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceCommand">DeviceHive RESTful API: DeviceCommand</a> for details.
 * Transport declared in the hive context will be used.
 */
public interface CommandsController {

    /**
     * Queries commands with following parameters:
     *
     * @param deviceGuid  device identifier
     * @param start       start timestamp
     * @param end         end timestamp
     * @param commandName filter by command
     * @param status      filter by status
     * @param sortField   either "Timestamp", "Command" or "Status"
     * @param sortOrder   ASC or DESC
     * @param take        number of entities to take
     * @param skip        number of entities that should be skiped (first 'skip' rows will be  skipped)
     * @return list of device commands
     */
    List<DeviceCommand> queryCommands(String deviceGuid, Timestamp start, Timestamp end, String commandName,
                                      String status, String sortField, String sortOrder, Integer take, Integer skip);

    /**
     * Get command with following parameters
     *
     * @param guid device identifiers
     * @param id   command identifier
     * @return Command resource associated with required id
     */
    DeviceCommand getCommand(String guid, long id);

    /**
     * Insert and send command to device with specified identifier.
     *
     * @param guid    device identifier
     * @param command command to be inserted
     * @return inserted command resource
     */
    DeviceCommand insertCommand(String guid, DeviceCommand command);

    /**
     * Updates command with specified id by device with specified identifier. Notifies client who sent this command
     * by command/update message.
     *
     * @param deviceGuid device identifier
     * @param id         command identifier
     * @param command    command resource
     */
    void updateCommand(String deviceGuid, long id, DeviceCommand command);

    /**
     * Subscribes client or device to commands. RESTful poll/pollMany or websocket subscribe will be used. When
     * command proceed device will be notified by servers's command/update message.
     *
     * @param timestamp start timestamp
     * @param names     names of the commands
     * @param deviceIds device identifiers
     */
    void subscribeForCommands(Timestamp timestamp, Set<String> names, String... deviceIds);

    /**
     * Unsubscribes client or device from commands. In case of websocket unsubscribe method will be used,
     * otherwise polling thread must be terminated.
     *
     * @param names     names of commands
     * @param deviceIds device identifiers
     */
    void unsubscribeFromCommands(Set<String> names, String... deviceIds);

    /**
     * Return commands queue
     *
     * @return commands queue
     */
    Queue<Pair<String, DeviceCommand>> getCommandQueue();

    /**
     * Return command updates queue
     *
     * @return command updates queue
     */
    Queue<DeviceCommand> getCommandUpdatesQueue();
}
