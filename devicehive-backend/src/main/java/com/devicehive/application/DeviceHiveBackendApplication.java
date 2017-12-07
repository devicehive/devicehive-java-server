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

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(value = "com.devicehive", excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.springframework.transaction.*")})
public class DeviceHiveBackendApplication {

    public static void main(String... args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(DeviceHiveBackendApplication.class)
                .web(false)
                .run(args);

        context.registerShutdownHook();
    }

}
