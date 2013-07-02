package com.devicehive.websockets.messagebus.global;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.*;
import javax.transaction.Transactional;
import java.io.Serializable;


@Singleton
public class MessagePublisher {


    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);
    @Inject
    @JMSConnectionFactory(Constants.JMS_TOPIC_FACTORY)
    private JMSContext context;
    @Resource(mappedName = Constants.JMS_COMMAND_TOPIC)
    private Topic commandTopic;
    @Resource(mappedName = Constants.JMS_COMMAND_UPDATE_TOPIC)
    private Topic commandUpdateTopic;
    @Resource(mappedName = Constants.JMS_NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    @Transactional(Transactional.TxType.MANDATORY)
    public void publishCommand(DeviceCommand deviceCommand) {
        publishObjectToTopic(deviceCommand, commandTopic);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void publishCommandUpdate(DeviceCommand deviceCommand) {
        publishObjectToTopic(deviceCommand, commandUpdateTopic);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void publishNotification(DeviceNotification deviceNotification) {
        publishObjectToTopic(deviceNotification, notificationTopic);
    }

    private void publishObjectToTopic(Serializable object, Topic topic) {
        JMSProducer producer = context.createProducer();
        producer.send(topic, object);
    }


}
