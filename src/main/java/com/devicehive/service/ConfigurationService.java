package com.devicehive.service;

import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.model.Configuration;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@Startup
public class ConfigurationService {

    private ConcurrentMap<String, String> configurationMap = new ConcurrentHashMap<>();

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
    public String getProperty(String name){
        return configurationMap.get(name);
    }

    @Lock(LockType.READ)
    public Map<String, String> getAllProperties(){
        return configurationMap;
    }

    private void setProperty(@NotNull String name, @NotNull String value){
        if (configurationDAO.updateConfiguration(name, value)){
            configurationMap.replace(name, value);
        }  else {
            configurationDAO.insertConfiguration(new Configuration(name, value));
            configurationMap.put(name, value);
        }
    }

    @Lock(LockType.WRITE)
    public void setProperty(@NotNull Configuration configuration){
        if (configurationDAO.updateConfiguration(configuration.getName(), configuration.getValue())){
            configurationMap.replace(configuration.getName(), configuration.getValue());
        }  else {
            configurationDAO.insertConfiguration(configuration);
            configurationMap.put(configuration.getName(), configuration.getValue());
        }
        configurationMap.put(configuration.getName(), configuration.getValue());
    }

    @Lock(LockType.WRITE)
    public void setProperties(Map<String, String> properties){
        for (String propertyName : properties.keySet()){
            setProperty(propertyName, properties.get(propertyName));
        }
    }
}
