package com.devicehive.application;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(value = "com.devicehive", excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.springframework.transaction.*")})
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
