package com.devicehive.messages.bus.global;

import java.io.IOException;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.bus.local.MessageBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.MessageType;

@JMSDestinationDefinition(
        name = Constants.JMS_COMMAND_TOPIC,
        interfaceName = "javax.jms.Topic",
        destinationName = Constants.COMMAND_TOPIC_DESTINATION_NAME)
@MessageDriven(mappedName = Constants.JMS_COMMAND_TOPIC)
public class CommandMessageHandler implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CommandMessageHandler.class);

    @Inject
    private MessageBus messageBus;

    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            DeviceCommand deviceCommand = (DeviceCommand) objectMessage.getObject();
            if (deviceCommand != null) {
                logger.debug("DeviceCommand received: " + deviceCommand);
                messageBus.send(MessageType.CLIENT_TO_DEVICE_COMMAND, deviceCommand);
            }
        }
        catch (JMSException e) {
            logger.error("[onMessage] Error processing command. ", e);
        }
        catch (IOException e) {
            logger.error("[onMessage] Error processing command. ", e);
        }
    }
}
