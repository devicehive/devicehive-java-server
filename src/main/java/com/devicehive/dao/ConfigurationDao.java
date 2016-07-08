package com.devicehive.dao;

import com.devicehive.model.Configuration;

import java.util.Optional;

public interface ConfigurationDao {
    Optional<Configuration> getByName(String name);

    int delete(String name);

    Configuration find(Long id);

    void persist(Configuration configuration);

    Configuration merge(Configuration existing);
}
