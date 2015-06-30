package com.devicehive.base.bean;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.messages.kafka.CommandConsumer;
import com.devicehive.messages.kafka.CommandUpdateConsumer;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.messages.kafka.NotificationConsumer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.ExecutorService;

public class TestKafkaProducer implements KafkaProducer {

    @Autowired
    private NotificationConsumer notificationConsumer;

    @Autowired
    private CommandConsumer commandConsumer;

    @Autowired
    private CommandUpdateConsumer commandUpdateConsumer;

    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService executorService;

    @Override
    public void produceDeviceNotificationMsg(DeviceNotification message, String topicName) {
        executorService.submit(() -> notificationConsumer.submitMessage(message));
    }

    @Override
    public void produceDeviceCommandMsg(DeviceCommand message, String topicName) {
        executorService.submit(() -> commandConsumer.submitMessage(message));
    }

    @Override
    public void produceDeviceCommandUpdateMsg(DeviceCommand message, String topicName) {
        executorService.submit(() -> commandUpdateConsumer.submitMessage(message));
    }
}
