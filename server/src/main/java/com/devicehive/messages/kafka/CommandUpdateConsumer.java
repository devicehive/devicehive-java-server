package com.devicehive.messages.kafka;

import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommandMessage;
import com.devicehive.service.DeviceService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.util.SessionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.websocket.Session;
import java.util.Set;

/**
 * Created by tmatvienko on 1/30/15.
 */
@Stateless
@LogExecutionTime
public class CommandUpdateConsumer extends AbstractConsumer<DeviceCommandMessage> {
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
    public void submitMessage(DeviceCommandMessage message) {
        LOGGER.debug("Device command update was submitted: {}", message.getId());

        if (message.getOriginSessionId() != null) {
            Session session = sessionMonitor.getSession(message.getOriginSessionId());
            if (session != null) {
                mes.submit(WebsocketHandlerCreator.createCommandUpdate(session).getHandler(message, null));
            }
        }

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                .getByCommandId(message.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            mes.submit(commandUpdateSubscription.getHandlerCreator()
                    .getHandler(message, commandUpdateSubscription.getSubscriptionId()));
        }
    }
}
