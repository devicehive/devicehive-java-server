package com.devicehive.websockets.messagebus.global;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.jms.*;
import javax.management.JMException;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 14.06.13
 * Time: 12:14
 * To change this template use File | Settings | File Templates.
 */
@Singleton
public class MessagePublisher {


    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);

    @Resource(mappedName = "jms/TopicFactory")
    private TopicConnectionFactory  connectionFactory;

    @Resource(mappedName = "jms/CommandTopic")
    private Topic commandTopic;

    @Resource(mappedName = "jms/CommandUpdateTopic")
    private Topic commandUpdateTopic;

    @Resource(mappedName = "jms/NotificationTopic")
    private Topic notificationTopic;

    private TopicConnection topicConnection;


    @PostConstruct
    public void postConstruct(){
        try {
            topicConnection = connectionFactory.createTopicConnection();
        } catch (JMSException e) {
            logger.error("Can not open JMS connection");
            throw new RuntimeException(e); //TODO
        }
    }


    @PreDestroy
    public void preDestroy(){
        try {
            topicConnection.close();
        } catch (JMSException e) {
            logger.error("Can not close JMS connection");
        }
    }


    public void publishCommand(DeviceCommand deviceCommand) throws JMSException {
        publishObjectToTopic(deviceCommand, commandTopic);
    }


    public void publishCommandUpdate(DeviceCommand deviceCommand) throws JMSException {
        publishObjectToTopic(deviceCommand, commandUpdateTopic);
    }

    public void publishNotification(DeviceNotification deviceNotification) throws JMSException {
        publishObjectToTopic(deviceNotification, notificationTopic);
    }


    private void publishObjectToTopic(Serializable object, Topic topic) throws JMSException {
        try(Session session = topicConnection.createTopicSession(true, Session.AUTO_ACKNOWLEDGE)) {
            MessageProducer messageProducer = session.createProducer(topic);
            ObjectMessage message = session.createObjectMessage(object);
            messageProducer.send(message);
        }
    }



}
