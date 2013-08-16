package com.devicehive.configuration;

import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.model.Configuration;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@Startup
public class ConfigurationStorage {

    private ConcurrentMap<String, String> configurationMap;

    @EJB
    private ConfigurationDAO configurationDAO;

    @PostConstruct
    public void init(){
        List<Configuration> existingConfigs = configurationDAO.findAll();
        configurationMap = new ConcurrentHashMap<>(existingConfigs.size());
        for (Configuration configuration : existingConfigs){
            configurationMap.put(configuration.getName(), configuration.getValue());
        }
    }

    @Lock(LockType.READ)
    public void readProperty(String name){
        Configuration config = configurationDAO.findByName(name);
        configurationMap.put(config.getName(), config.getValue());
    }

    @Lock(LockType.WRITE)
    public void readAll(){
       List<Configuration> configurations = configurationDAO.findAll();
       configurationMap.clear();
       for (Configuration currentConfig : configurations){
           configurationMap.put(currentConfig.getName(), currentConfig.getValue());
       }
    }

    @Lock(LockType.READ)
    public String get (String name){
        return configurationMap.get(name);
    }

}
