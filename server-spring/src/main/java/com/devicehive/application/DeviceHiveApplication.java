package com.devicehive.application;

import com.devicehive.util.ApplicationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.annotation.PostConstruct;

@SpringBootApplication(exclude = { JacksonAutoConfiguration.class })
@ComponentScan("com.devicehive")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
public class DeviceHiveApplication {
    public static final String WAIT_EXECUTOR = "DeviceHiveWaitService";
    public static final String MESSAGE_EXECUTOR = "DeviceHiveMessageService";

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void initApp() {
        ApplicationContextHolder.getInstance().set(context);
    }

    public static void main(String ... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }

}
