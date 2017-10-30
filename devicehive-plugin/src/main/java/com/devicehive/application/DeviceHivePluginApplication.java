package com.devicehive.application;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.json.GsonFactory;
import com.google.gson.Gson;
import io.swagger.jaxrs.config.BeanConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ComponentScan(value = "com.devicehive", excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.springframework.transaction.*")
})
@ServletComponentScan("com.devicehive.application.filter")
@EnableScheduling
@EnableCaching
@EnableAsync(proxyTargetClass = true)
public class DeviceHivePluginApplication extends SpringBootServletInitializer {

    public static final String MESSAGE_EXECUTOR = "DeviceHiveMessageService";

    public static void main(String... args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(DeviceHivePluginApplication.class)
                .web(true)
                .run(args);

        context.registerShutdownHook();
    }

    @Bean
    @Lazy(false)
    public BeanConfig swaggerConfig(@Value("${server.context-path}") String contextPath,
                                    @Value("${build.version}") String buildVersion) {

        String basePath = contextPath.equals("/") ? JerseyConfig.REST_PATH : contextPath + JerseyConfig.REST_PATH;
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Device Hive REST API");
        beanConfig.setVersion(buildVersion);
        beanConfig.setBasePath(basePath);
        beanConfig.setResourcePackage("com.devicehive.resource");
        beanConfig.setScan(true);
        return beanConfig;
    }

    @Lazy(false)
    @Bean(name = MESSAGE_EXECUTOR)
    public ExecutorService messageExecutorService(@Value("${app.executor.size:1}") Integer executorSize) {
        return Executors.newFixedThreadPool(executorSize);
    }

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    public Validator localValidator() {
        return new LocalValidatorFactoryBean();
    }
}
