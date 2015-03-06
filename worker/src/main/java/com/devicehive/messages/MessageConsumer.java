package com.devicehive.messages;

import com.devicehive.domain.DeviceCommand;
import com.devicehive.domain.DeviceNotification;
import com.devicehive.domain.wrappers.DeviceCommandWrapper;
import com.devicehive.domain.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.DeviceCommandService;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Created by tmatvienko on 2/5/15.
 */
@Component
public class MessageConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    @Autowired
    private CassandraOperations cassandraTemplate;
    @Autowired
    private DeviceCommandService commandService;

    @Async
    public void subscribeOnNotifications(KafkaStream a_stream, int a_threadNumber) throws InterruptedException {
        LOGGER.info("{}: Kafka device notifications consumer started... {} ", Thread.currentThread().getName(), a_threadNumber);
        ConsumerIterator<String, DeviceNotificationWrapper> it = a_stream.iterator();
        while (it.hasNext()) {
            DeviceNotificationWrapper message = it.next().message();
            LOGGER.debug("Notification {}: Thread {}: {}", Thread.currentThread().getName(), a_threadNumber, message);
            final DeviceNotification notification = new DeviceNotification(message);
            cassandraTemplate.insertAsynchronously(notification);
        }
        LOGGER.info("Shutting down Thread: " + a_threadNumber);
    }

    @Async
    public void subscribeOnCommands(KafkaStream a_stream, int a_threadNumber) throws InterruptedException {
        LOGGER.info("{}: Kafka device commands consumer started... {} ", Thread.currentThread().getName(), a_threadNumber);
        ConsumerIterator<String, DeviceCommandWrapper> it = a_stream.iterator();
        while (it.hasNext()) {
            DeviceCommandWrapper message = it.next().message();
            LOGGER.debug("Command {}: Thread {}: {}", Thread.currentThread().getName(), a_threadNumber, message);
            DeviceCommand command = new DeviceCommand(message);
            command.setIsUpdated(false);
            cassandraTemplate.insert(command);
        }
        LOGGER.info("Shutting down Thread: " + a_threadNumber);
    }

    @Async
    public void subscribeOnCommandsUpdate(KafkaStream a_stream, int a_threadNumber) throws InterruptedException {
        LOGGER.info("{}: Kafka device command update consumer started... {} ", Thread.currentThread().getName(), a_threadNumber);
        ConsumerIterator<String, DeviceCommandWrapper> it = a_stream.iterator();
        while (it.hasNext()) {
            DeviceCommandWrapper message = it.next().message();
            LOGGER.debug("CommandUpdate {}: Thread {}: {}", Thread.currentThread().getName(), a_threadNumber, message);
            DeviceCommand command = new DeviceCommand(message);
            commandService.updateDeviceCommmand(command);
        }
        LOGGER.info("Shutting down Thread: " + a_threadNumber);
    }
}
