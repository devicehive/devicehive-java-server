package com.devicehive.service;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.Timer;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebsocketSession;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import javax.websocket.Session;
import java.sql.Timestamp;
import java.util.List;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;


@Stateless
@LogExecutionTime
public class DeviceCommandService {

    private DeviceCommandDAO commandDAO;
    private DeviceCommandService self;
    private GlobalMessageBus globalMessageBus;
    private AsyncMessageSupplier asyncMessageDeliverer;
    private SubscriptionManager subscriptionManager;

    @EJB
    public void setSubscriptionManager(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @EJB
    public void setAsyncMessageDeliverer(AsyncMessageSupplier asyncMessageDeliverer) {
        this.asyncMessageDeliverer = asyncMessageDeliverer;
    }

    @EJB
    public void setGlobalMessageBus(GlobalMessageBus globalMessageBus) {
        this.globalMessageBus = globalMessageBus;
    }

    @EJB
    public void setSelf(DeviceCommandService self) {
        this.self = self;
    }

    @EJB
    public void setCommandDAO(DeviceCommandDAO commandDAO) {
        this.commandDAO = commandDAO;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getWithDevice(@NotNull long id) {
        return commandDAO.getWithDevice(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getWithDeviceAndUser(@NotNull long id) {
        return commandDAO.getWithDeviceAndUser(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getByGuidAndId(@NotNull String guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand findById(Long id) {
        return commandDAO.findById(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> getNewerThan(List<Device> devices, User user, Timestamp timestamp) {
        return commandDAO.getCommandsListForPolling(devices, null, user, timestamp);
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> queryDeviceCommand(Device device, Timestamp start, Timestamp end, String command,
                                                  String status, String sortField, Boolean sortOrderAsc,
                                                  Integer take, Integer skip, Integer gridInterval) {
        return commandDAO.queryDeviceCommand(device, start, end, command, status, sortField, sortOrderAsc, take,
                skip, gridInterval);
    }

    public DeviceCommand getByDeviceGuidAndId(@NotNull String guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {
        Timer timer = Timer.newInstance();
        DeviceCommand saved = self.saveDeviceCommandUpdate(update, device);
        timer.logMethodExecuted("DeviceCommandService.self.saveDeviceCommandUpdate");
        globalMessageBus.publishDeviceCommandUpdate(saved);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceCommand(DeviceCommand command, Device device, User user, final Session session) {
        Timer timer = Timer.newInstance();
        self.saveDeviceCommand(command, device, user, session);
        timer.logMethodExecuted("DeviceCommandService.self.saveDeviceCommand");
        globalMessageBus.publishDeviceCommand(command);
    }

    public void saveDeviceCommand(final DeviceCommand command, Device device, User user, final Session session) {
        command.setDevice(device);
        command.setUser(user);
        commandDAO.createCommand(command);
        if (session != null) {
            Runnable removeHandler = new Runnable() {
                @Override
                public void run() {
                    subscriptionManager.getCommandUpdateSubscriptionStorage().remove(command.getId(), session.getId());
                }
            };
            CommandUpdateSubscription commandUpdateSubscription =
                    new CommandUpdateSubscription(command.getId(), session.getId(),
                            new WebsocketHandlerCreator(session, WebsocketSession.COMMAND_UPDATES_SUBSCRIPTION_LOCK,
                                    asyncMessageDeliverer, removeHandler));
            subscriptionManager.getCommandUpdateSubscriptionStorage().insert(commandUpdateSubscription);
        }
    }

    public DeviceCommand saveDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {

        DeviceCommand cmd = commandDAO.findById(update.getId());

        if (cmd == null) {
            throw new HiveException("Command not found!", NOT_FOUND.getStatusCode());
        }

        if (!cmd.getDevice().getId().equals(device.getId())) {
            throw new HiveException("Device tries to update incorrect command", UNAUTHORIZED.getStatusCode());
        }


        if (update.getCommand() != null) {
            cmd.setCommand(update.getCommand().getValue());
        }
        if (update.getFlags() != null) {
            cmd.setFlags(update.getFlags().getValue());
        }
        if (update.getLifetime() != null) {
            cmd.setLifetime(update.getLifetime().getValue());
        }
        if (update.getParameters() != null) {
            cmd.setParameters(update.getParameters().getValue());
        }
        if (update.getResult() != null) {
            cmd.setResult(update.getResult().getValue());
        }
        if (update.getStatus() != null) {
            cmd.setStatus(update.getStatus().getValue());
        }
        if (update.getTimestamp() != null) {
            cmd.setTimestamp(update.getTimestamp().getValue());
        }
        return cmd;
    }

}
