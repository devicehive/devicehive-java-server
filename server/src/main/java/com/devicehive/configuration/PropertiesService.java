package com.devicehive.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by tmatvienko on 11/19/14.
 */
@Startup
@Singleton
public class PropertiesService {

    private static final String PROPERTIES = "/app.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesService.class);

    private Properties properties;

    @PostConstruct
    private void startup() {
        try (InputStream is = PropertiesService.class.getResourceAsStream(PROPERTIES)) {
            properties = new Properties();
            properties.load(is);
        } catch (IOException ex) {
            LOGGER.error("IOExeption has been caught during loading properties", ex);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }
}
