package com.devicehive.application.async;

import com.devicehive.application.DeviceHiveApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile({"default", "test"})
@EnableScheduling
@EnableAsync
public class DefaultAsyncConfig {

    @Bean(name = DeviceHiveApplication.WAIT_EXECUTOR)
    public ExecutorService waitExecutorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    }

    @Bean(name = DeviceHiveApplication.MESSAGE_EXECUTOR)
    public ExecutorService messageExecutorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    }

}
