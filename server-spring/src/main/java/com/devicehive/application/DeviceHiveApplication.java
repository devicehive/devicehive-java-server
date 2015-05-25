package com.devicehive.application;

import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.util.ApplicationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.annotation.PostConstruct;
import javax.validation.Validator;

@SpringBootApplication(exclude = { JacksonAutoConfiguration.class })
@ComponentScan("com.devicehive")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EntityScan(basePackages = {"com.devicehive.model"})
public class DeviceHiveApplication {
    public static final String WAIT_EXECUTOR = "DeviceHiveWaitService";
    public static final String MESSAGE_EXECUTOR = "DeviceHiveMessageService";

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void initApp() {
        ApplicationContextHolder.getInstance().set(context);
    }

    @Bean
    public Validator localValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public HiveSecurityContext securityContext() {
        return new HiveSecurityContext();
    }

    public static void main(String ... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }

}
