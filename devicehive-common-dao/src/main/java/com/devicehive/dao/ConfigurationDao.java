package com.devicehive.dao;

import com.devicehive.vo.ConfigurationVO;

import java.util.Optional;

public interface ConfigurationDao {

    Optional<ConfigurationVO> getByName(String name);

    int delete(String name);

    void persist(ConfigurationVO configuration);

    ConfigurationVO merge(ConfigurationVO existing);
}
