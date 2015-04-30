package com.devicehive;

import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.domain.DeviceCommand;
import com.devicehive.repository.DeviceCommandRepository;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by tmatvienko on 2/13/15.
 */
public class CommandRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DeviceCommandRepository commandRepository;

    @Test
    public void repositoryStoresAndRetrievesEvents() {
        final String id = String.valueOf(UUIDs.timeBased().timestamp());
        final DeviceCommand command1 = new DeviceCommand(id, deviceGuid, date, "command1", null, null, null, null, null, null, null);
        final DeviceCommand command2 = new DeviceCommand(String.valueOf(UUIDs.timeBased()), deviceGuid2, date, "command2", null, null, null, null, null, null, null);
        commandRepository.save(ImmutableSet.of(command1, command2));

        Iterable<DeviceCommand> commands = commandRepository.findByDeviceGuids(deviceGuid);
        assertThat(commands, hasItem(command1));

        commands = commandRepository.findByDeviceGuids(deviceGuid2);
        assertThat(commands, hasItem(command2));
    }

    @Test
    public void repositoryDeletesStoredEvents() {
        final DeviceCommand command1 = new DeviceCommand(String.valueOf(UUIDs.timeBased()), deviceGuid, date, "command", null, null, null, null, null, null, null);
        final DeviceCommand command2 = new DeviceCommand(String.valueOf(UUIDs.timeBased()), deviceGuid2, date, "command", null, null, null, null, null, null, null);
        commandRepository.save(ImmutableSet.of(command1, command2));

        commandRepository.delete(command1);
        commandRepository.delete(command2);

        Iterable<DeviceCommand> notifications = commandRepository.findByDeviceGuids(deviceGuid);
        assertThat(notifications, not(hasItem(command1)));

        notifications = commandRepository.findByDeviceGuids(deviceGuid2);
        assertThat(notifications, not(hasItem(command2)));
    }
}
