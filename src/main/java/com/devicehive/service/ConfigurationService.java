package com.devicehive.service;

import com.devicehive.configuration.ConfigurationStorage;
import com.devicehive.dao.ConfigurationDAO;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.validation.constraints.NotNull;

@Singleton
@Startup
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    private static final String SERVER_CONFIGURATION_NOTIFICATION = "SERVER_CONFIGURATION_NOTIFICATION";

    @EJB
    private ConfigurationDAO configurationDAO;
    @EJB
    private HazelcastService hazelcastService;
    @EJB
    private ConfigurationStorage configurationStorage;

    private HazelcastInstance hazelcast;

    public void save(@NotNull String name, @NotNull String value) {
        configurationDAO.save(name, value);
        logger.debug("Sending server configuration notification {}", name);
        hazelcast.getTopic(SERVER_CONFIGURATION_NOTIFICATION).publish(name);
        logger.debug("Sent");
    }

    @PostConstruct
    public void init() {
        hazelcast = hazelcastService.getHazelcast();
        logger.debug("Initializing topic {}...", SERVER_CONFIGURATION_NOTIFICATION);
        ITopic<String> serverConfigurationNotificationTopic = hazelcast.getTopic
                (SERVER_CONFIGURATION_NOTIFICATION);
        serverConfigurationNotificationTopic.addMessageListener(new ConfigurationListener(configurationStorage));
        logger.debug("Done {}", SERVER_CONFIGURATION_NOTIFICATION);
    }

    private static class ConfigurationListener implements MessageListener<String> {

        private final ConfigurationStorage configurationStorage;

        private ConfigurationListener(ConfigurationStorage configurationStorage) {
            this.configurationStorage = configurationStorage;
        }

        @Override
        public void onMessage(Message<String> configurationMessage) {
            logger.debug("Received configuration{}", configurationMessage.getMessageObject());
            configurationStorage.readProperty(configurationMessage.getMessageObject());
        }
    }
}
