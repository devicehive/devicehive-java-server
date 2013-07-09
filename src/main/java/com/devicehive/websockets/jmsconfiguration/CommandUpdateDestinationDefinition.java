package com.devicehive.websockets.jmsconfiguration;

import com.devicehive.configuration.Constants;

import javax.ejb.Singleton;
import javax.jms.JMSDestinationDefinition;

@JMSDestinationDefinition(
        name= Constants.JMS_COMMAND_UPDATE_TOPIC,
        interfaceName = "javax.jms.Topic",
        destinationName = Constants.COMMAND_UPDATE_TOPIC_DESTINATION_NAME
)
@Singleton
public class CommandUpdateDestinationDefinition {
}
