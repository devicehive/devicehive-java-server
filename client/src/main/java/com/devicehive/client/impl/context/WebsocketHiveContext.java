package com.devicehive.client.impl.context;

import com.devicehive.client.MessageHandler;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.util.UUID;


public class WebsocketHiveContext extends RestHiveContext {

    private static Logger logger = LoggerFactory.getLogger(HiveRestConnector.class);

    private HiveWebsocketConnector websocketConnector;
    private final ConnectionDescriptor connectionDescriptor;

    private SubscriptionFilter commandsFilter;
    private SubscriptionFilter notificationsFilter;

    /**
     * @param commandsHandler       handler for incoming commands and command updates
     * @param commandUpdatesHandler
     * @param notificationsHandler  handler for incoming notifications
     */
    public WebsocketHiveContext(ConnectionDescriptor connectionDescriptor,
                                MessageHandler<DeviceCommand> commandsHandler,
                                MessageHandler<DeviceCommand> commandUpdatesHandler,
                                MessageHandler<DeviceNotification> notificationsHandler) throws HiveException {
        super(connectionDescriptor, commandsHandler, commandUpdatesHandler, notificationsHandler);
        this.connectionDescriptor = connectionDescriptor;
        open();
    }

    private void open() throws HiveException {
        try {
            this.websocketConnector = new HiveWebsocketConnector(connectionDescriptor.getWebsocketURI(), this);
        } catch (IOException|DeploymentException e) {
            throw new HiveException("Error creating context", e);
        }
    }


    public synchronized void reconnect() throws HiveException {
        this.websocketConnector.close();
        open();
        authenticate(getHivePrincipal());
        addCommandsSubscription(null);
        addNotificationsSubscription(null);
    }


    public synchronized void close() {
        try {
            websocketConnector.close();
        } catch (Exception ex) {
            logger.error("Error closing Websocket client", ex);
        }
        super.close();
    }

    public synchronized HiveWebsocketConnector getWebsocketConnector() {
        return websocketConnector;
    }

    @Override
    public ApiInfo getInfo() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "server/info");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        ApiInfo apiInfo = getWebsocketConnector().sendMessage(request, "info", ApiInfo.class, null);
        String restUrl = apiInfo.getRestServerUrl();
        apiInfo = super.getInfo();
        apiInfo.setRestServerUrl(restUrl);
        return apiInfo;
    }

    @Override
    public synchronized void addCommandsSubscription(SubscriptionFilter newFilter) throws HiveException {
        removeCommandsSubscription();
        this.commandsFilter = ObjectUtils.cloneIfPossible(newFilter);
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        websocketConnector.sendMessage(request);
    }

    @Override
    public synchronized void addNotificationsSubscription(SubscriptionFilter newFilter) throws HiveException {
        removeNotificationsSubscription();
        this.notificationsFilter = ObjectUtils.cloneIfPossible(newFilter);
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        websocketConnector.sendMessage(request);
    }

    @Override
    public synchronized void removeCommandsSubscription() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/unsubscribe");
        websocketConnector.sendMessage(request);
        this.commandsFilter = null;
    }

    @Override
    public synchronized void removeNotificationsSubscription() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/unsubscribe");
        websocketConnector.sendMessage(request);
        this.notificationsFilter = null;
    }

    @Override
    public synchronized void authenticate(HivePrincipal principal) throws HiveException {
        super.authenticate(principal);
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        if (principal.getUser() != null) {
            request.addProperty("login", principal.getUser().getKey());
            request.addProperty("password", principal.getUser().getValue());
        } else if (principal.getDevice() != null) {
            request.addProperty("deviceId", principal.getDevice().getKey());
            request.addProperty("deviceKey", principal.getDevice().getValue());
        } else if (principal.getAccessKey() != null) {
            request.addProperty("accessKey", principal.getAccessKey());
        } else {
            throw new IllegalArgumentException("Incorrect HivePrincipal was passed");
        }
        websocketConnector.sendMessage(request);
    }
}
