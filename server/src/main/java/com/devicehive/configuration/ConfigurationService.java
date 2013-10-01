package com.devicehive.configuration;

import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.model.Configuration;
import com.devicehive.service.HazelcastService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@Startup
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private static final String SERVER_CONFIGURATION_NOTIFICATION = "SERVER_CONFIGURATION_NOTIFICATION";

    @Resource
    private SessionContext sessionContext;

    @EJB
    private ConfigurationDAO configurationDAO;
    @EJB
    private HazelcastService hazelcastService;

    private HazelcastInstance hazelcast;

    private ConcurrentMap<String, String> configurationMap;


    @PostConstruct
    public void init() {
        configurationMap = new ConcurrentHashMap<>();
        updateAll();
        hazelcast = hazelcastService.getHazelcast();
        logger.debug("Initializing topic {}...", SERVER_CONFIGURATION_NOTIFICATION);
        ITopic<String> serverConfigurationNotificationTopic = hazelcast.getTopic
                (SERVER_CONFIGURATION_NOTIFICATION);
        serverConfigurationNotificationTopic.addMessageListener(new ConfigurationListener());
        logger.debug("Done {}", SERVER_CONFIGURATION_NOTIFICATION);

    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T> void save(@NotNull String name, T value) {
        String str = value != null ? value.toString() : null;
        configurationDAO.save(name, str);
        update(name);
        logger.debug("Sending server configuration notification {}", name);
        hazelcast.getTopic(SERVER_CONFIGURATION_NOTIFICATION).publish(name);
        logger.debug("Sent");
    }

    @Lock(LockType.WRITE)
    public <T> void update(@NotNull String name) {
        Configuration configuration = configurationDAO.findByName(name);
        if (configuration != null) {
            configurationMap.put(configuration.getName(), configuration.getValue());
        }
    }

    @Lock(LockType.WRITE)
    public <T> void updateAll() {
        List<Configuration> existingConfigs = configurationDAO.findAll();
        configurationMap.clear();
        for (Configuration configuration : existingConfigs) {
            configurationMap.put(configuration.getName(), configuration.getValue());
        }
    }

    @Lock(LockType.WRITE)
    public <T> void notifyUpdateAll() {
        updateAll();
        hazelcast.getTopic(SERVER_CONFIGURATION_NOTIFICATION).publish(null);
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String get(@NotNull String name) {
        return configurationMap.get(name);
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getLong(@NotNull String name, long defaultValue) {
        String val = configurationMap.get(name);
        return val != null ? Long.parseLong(val) : defaultValue;
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int getInt(@NotNull String name, int defaultValue) {
        String val = configurationMap.get(name);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean getBoolean(@NotNull String name, boolean defaultValue) {
        String val = configurationMap.get(name);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }


    private class ConfigurationListener implements MessageListener<String> {


        private ConfigurationListener() {
        }

        @Override
        public void onMessage(Message<String> message) {
            logger.debug("Received configuration {}", message.getMessageObject());
            if (!message.getPublishingMember().localMember()) {
                String name = message.getMessageObject();
                ConfigurationService configurationService =
                        sessionContext.getBusinessObject(ConfigurationService.class);
                if (name != null) {
                    configurationService.update(name);
                } else {
                    configurationService.updateAll();
                }
            }
        }
    }
}
