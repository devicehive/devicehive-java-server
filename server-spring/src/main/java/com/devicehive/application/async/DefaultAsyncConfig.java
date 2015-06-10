package com.devicehive.application.async;

import com.devicehive.application.DeviceHiveApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Both executors configured to have pool size of 32 and this setting is taken from glassfish
 * fixme: should think how to change long poling implementation
 *
 */
@Configuration
@Profile({"!jee-container"})
public class DefaultAsyncConfig {

    @Bean(name = DeviceHiveApplication.WAIT_EXECUTOR)
    public ExecutorService waitExecutorService() {
        return Executors.newFixedThreadPool(32);
    }

    @Bean(name = DeviceHiveApplication.MESSAGE_EXECUTOR)
    public ExecutorService messageExecutorService() {
        return Executors.newFixedThreadPool(32);
    }

}
