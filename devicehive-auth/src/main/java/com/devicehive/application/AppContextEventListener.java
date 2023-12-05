package com.devicehive.application;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppContextEventListener implements ApplicationListener<ApplicationEvent> {

    private void printActiveProperties(ConfigurableEnvironment env) {

        System.out.println("************************* ACTIVE APP PROPERTIES ******************************");

        List<MapPropertySource> propertySources = new ArrayList<>();

        env.getPropertySources().forEach(it -> {
            if (it instanceof MapPropertySource) {
                propertySources.add((MapPropertySource) it);
            }
        });

        propertySources.stream()
                .map(propertySource -> propertySource.getSource().keySet())
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .forEach(key -> {
                    try {
                        System.out.println(key + "=" + env.getProperty(key));
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                });
        System.out.println("******************************************************************************");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ApplicationPreparedEvent) {
            printActiveProperties((ConfigurableEnvironment)  ((ApplicationPreparedEvent) event).getApplicationContext().getEnvironment());
        }
    }
}