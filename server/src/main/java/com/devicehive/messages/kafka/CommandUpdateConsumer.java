package com.devicehive.messages.kafka;

import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.service.DeviceService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.util.SessionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.Set;

/**
 * Created by tmatvienko on 1/30/15.
 */
@Stateless
@LogExecutionTime
public class CommandUpdateConsumer extends AbstractConsumer<DeviceCommand> {
    public static final Logger LOGGER = LoggerFactory.getLogger(CommandUpdateConsumer.class);

    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @EJB
    private SessionMonitor sessionMonitor;
    @Resource(name = "concurrent/DeviceHiveMessageService")
    private ManagedExecutorService mes;

    @Override
    public void submitMessage(DeviceCommand message) {
        LOGGER.debug("Device command update was submitted: {}", message.getId());

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                .getByCommandId(message.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            mes.submit(commandUpdateSubscription.getHandlerCreator()
                    .getHandler(message, commandUpdateSubscription.getSubscriptionId()));
        }
    }
}
