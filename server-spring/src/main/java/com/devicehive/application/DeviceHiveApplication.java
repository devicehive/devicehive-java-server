package com.devicehive.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@SpringBootApplication
@ComponentScan("com.devicehive")
public class DeviceHiveApplication {

    public static void main(String ... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }


}
