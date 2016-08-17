package com.devicehive.application;

import com.devicehive.service.MyServiceBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ComponentScan(value = "com.devicehive")
public class DeviceHiveApplication extends SpringBootServletInitializer {

    public static void main(String... args) {
        ConfigurableApplicationContext run = SpringApplication.run(DeviceHiveApplication.class);

        ConfigurableListableBeanFactory beanFactory = run.getBeanFactory();

        ((MyServiceBean) beanFactory.getBean("myServiceBean")).printMethod();

    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DeviceHiveApplication.class);
    }

}
