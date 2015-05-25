package com.devicehive.messages.kafka;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created by tmatvienko on 1/30/15.
 */
@Component
public class CommandUpdateConsumer extends AbstractConsumer<DeviceCommand> {
    private static final Logger logger = LoggerFactory.getLogger(CommandUpdateConsumer.class);

    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService mes;

    @Override
    public void submitMessage(DeviceCommand message) {
        logger.debug("Device command update was submitted: {}", message.getId());

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                .getByCommandId(message.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            mes.submit(commandUpdateSubscription.getHandlerCreator()
                    .getHandler(message, commandUpdateSubscription.getSubscriptionId()));
        }
    }
}
