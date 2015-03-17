package com.devicehive.dao;

/**
 * Created by tatyana on 3/16/15.
 */
public interface IConfigurationDAO {

    public String getStringConfig(String configName);

    public Integer getIntegerConfig(String configName);
}
