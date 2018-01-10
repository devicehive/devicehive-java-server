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

import com.devicehive.model.eventbus.FilterRegistry;
import com.devicehive.json.GsonFactory;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.devicehive.eventbus.EventBus;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class BackendConfig {

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    @DependsOn("filterRegistry")
    public EventBus eventBus(MessageDispatcher dispatcher, FilterRegistry filterRegistry) {
        return new EventBus(dispatcher, filterRegistry);
    }
}
