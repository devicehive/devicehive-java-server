package com.devicehive.websockets.messagebus.global;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.jms.*;
import javax.transaction.Transactional;
import java.io.Serializable;


@Singleton
public class MessagePublisher {


    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);

    @Resource(mappedName = Constants.JMS_TOPIC_FACTORY)
    private TopicConnectionFactory  connectionFactory;

    @Resource(mappedName = Constants.JMS_COMMAND_TOPIC)
    private Topic commandTopic;

    @Resource(mappedName =  Constants.JMS_COMMAND_UPDATE_TOPIC)
    private Topic commandUpdateTopic;

    @Resource(mappedName =  Constants.JMS_NOTIFICATION_TOPIC)
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

    @Transactional(Transactional.TxType.MANDATORY)
    public void publishCommand(DeviceCommand deviceCommand){
        publishObjectToTopic(deviceCommand, commandTopic);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void publishCommandUpdate(DeviceCommand deviceCommand){
        publishObjectToTopic(deviceCommand, commandUpdateTopic);
    }
    @Transactional(Transactional.TxType.MANDATORY)
    public void publishNotification(DeviceNotification deviceNotification) {
        publishObjectToTopic(deviceNotification, notificationTopic);
    }


    private void publishObjectToTopic(Serializable object, Topic topic)  {
        try(Session session = topicConnection.createTopicSession(true, Session.AUTO_ACKNOWLEDGE)) {
            MessageProducer messageProducer = session.createProducer(topic);
            ObjectMessage message = session.createObjectMessage(object);
            messageProducer.send(message);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }



}
