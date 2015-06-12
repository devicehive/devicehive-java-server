package com.devicehive.messages.kafka;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;

public interface KafkaProducer {

    void produceDeviceNotificationMsg(DeviceNotification message, String topicName);

    void produceDeviceCommandMsg(DeviceCommand message, String topicName);

    void produceDeviceCommandUpdateMsg(DeviceCommand message, String topicName);

}
