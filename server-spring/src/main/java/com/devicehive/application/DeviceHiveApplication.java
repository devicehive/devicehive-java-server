package com.devicehive.application;

import com.devicehive.util.ApplicationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.annotation.PostConstruct;
import javax.validation.Validator;

@SpringBootApplication(exclude = { JacksonAutoConfiguration.class })
@ComponentScan("com.devicehive")
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EntityScan(basePackages = {"com.devicehive.model"})
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class DeviceHiveApplication extends SpringBootServletInitializer {
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

    public static void main(String ... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DeviceHiveApplication.class);
    }
}
