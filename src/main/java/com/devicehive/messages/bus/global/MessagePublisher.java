package com.devicehive.messages.bus.global;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.bus.local.MessageListener;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Message;

@Singleton
public class MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);

    @Inject
    @JMSConnectionFactory(Constants.JMS_TOPIC_FACTORY)
    private JMSContext context;
    @Resource(lookup = Constants.JMS_COMMAND_TOPIC)
    private Topic commandTopic;
    @Resource(lookup = Constants.JMS_COMMAND_UPDATE_TOPIC)
    private Topic commandUpdateTopic;
    @Resource(lookup = Constants.JMS_NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    //Listeners used to notify about new messages
    private List<MessageListener> listeners = new ArrayList<>();

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

        //and let everyone know aboit new message!
        notifyListeners(object);
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    protected void notifyListeners(final Serializable message) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {

            @Override
            public void run() {
                for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                    MessageListener listener = (MessageListener) iterator.next();
                    try {
                        listener.messageAdded((Message) message);
                        iterator.remove();
                    }
                    catch (Exception e) {
                        logger.warn("Exception while notifying listener: " + listener);
                    }
                }
            }
        });
    }
}
