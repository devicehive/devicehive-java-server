package com.devicehive.application;

import io.swagger.jaxrs.config.BeanConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@SpringApplicationConfiguration( classes = {DeviceHiveApplicationConfiguration.class})
@ComponentScan(value = "com.devicehive", excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.springframework.transaction.*")})
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class DeviceHiveApplication extends SpringBootServletInitializer {

    public static final String MESSAGE_EXECUTOR = "DeviceHiveMessageService";

    public static void main(String ... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DeviceHiveApplication.class);
    }

    @Bean
    public Validator localValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Lazy(false)
    @Bean(name = MESSAGE_EXECUTOR)
    public ExecutorService messageExecutorService(@Value("${app.executor.size}") Integer executorSize) {
        return Executors.newFixedThreadPool(executorSize);
    }

    @Bean
    @Lazy(false)
    public BeanConfig swaggerConfig(@Value("${server.context-path}") String contextPath, @Value("${build.version}") String buildVersion) {
        String basePath = contextPath.equals("/") ? JerseyConfig.REST_PATH : contextPath + JerseyConfig.REST_PATH;
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Device Hive REST API");
        beanConfig.setVersion(buildVersion);
        beanConfig.setBasePath(basePath);
        beanConfig.setResourcePackage("com.devicehive.resource");
        beanConfig.setScan(true);
        return beanConfig;
    }
}
