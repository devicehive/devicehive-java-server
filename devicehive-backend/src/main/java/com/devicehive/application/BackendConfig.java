package com.devicehive.application;

/*
 * #%L
 * DeviceHive Backend Logic
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.devicehive.eventbus.EventBus;
import com.devicehive.shim.api.server.RpcServer;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Configuration
public class BackendConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    public EventBus eventBus(RpcServer rpcServer) {
        return new EventBus(rpcServer.getDispatcher());
    }

    @Bean
    @Lazy(false)
    public BeanConfig apiConfig(@Value("${build.version}") String buildVersion) {

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Device Hive REST API");
        beanConfig.setVersion(buildVersion);
        beanConfig.setBasePath(JerseyConfig.REST_PATH);
        beanConfig.setResourcePackage("com.devicehive.resource");
        beanConfig.setScan(true);
        return beanConfig;
    }

    @Bean
    public DataSourceHealthIndicator dbHealthIndicator() {
        return new DataSourceHealthIndicator(dataSource);
    }
}
