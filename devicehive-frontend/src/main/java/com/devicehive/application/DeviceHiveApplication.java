package com.devicehive.application;

import io.swagger.jaxrs.config.BeanConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@ComponentScan(value = "com.devicehive")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DeviceHiveApplication extends SpringBootServletInitializer {

    public static final String MESSAGE_EXECUTOR = "DeviceHiveMessageService";

    public static void main(String... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DeviceHiveApplication.class);
    }

    @Bean
    @Lazy(false)
    public BeanConfig swaggerConfig(@Value("${server.context-path}") String contextPath, @Value("${build.version}") String buildVersion) {
        String basePath = contextPath.equals("/") ? JerseyConfig.REST_PATH : contextPath + JerseyConfig.REST_PATH;
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Device Hive REST API");
        beanConfig.setVersion(buildVersion);
        beanConfig.setBasePath(basePath);
        beanConfig.setResourcePackage("com.devicehive.controller");
        beanConfig.setScan(true);
        return beanConfig;
    }

    @Lazy(false)
    @Bean(name = MESSAGE_EXECUTOR)
    public ExecutorService messageExecutorService(@Value("${app.executor.size:1}") Integer executorSize) {
        return Executors.newFixedThreadPool(executorSize);
    }
}
