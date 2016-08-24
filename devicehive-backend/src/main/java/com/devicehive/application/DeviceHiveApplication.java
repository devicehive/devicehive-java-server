package com.devicehive.application;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = "com.devicehive")
public class DeviceHiveApplication {

    public static void main(String... args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(DeviceHiveApplication.class)
                .web(false)
                .run(args);

        DeviceHiveApplication app = context.getBean(DeviceHiveApplication.class);
        context.registerShutdownHook();
    }

}
