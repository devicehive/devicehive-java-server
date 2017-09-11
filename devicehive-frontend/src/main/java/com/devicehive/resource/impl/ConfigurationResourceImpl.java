package com.devicehive.resource.impl;

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


import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.ConfigurationUpdate;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.resource.ConfigurationResource;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.ConfigurationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.util.Optional;

import static com.devicehive.configuration.Messages.CONFIG_NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

@Service
public class ConfigurationResourceImpl implements ConfigurationResource {

    private final ConfigurationService configurationService;
    private final HiveValidator hiveValidator;

    @Value("${server.context-path}")
    private String contextPath;

    @Autowired
    public ConfigurationResourceImpl(ConfigurationService configurationService, HiveValidator hiveValidator) {
        this.configurationService = configurationService;
        this.hiveValidator = hiveValidator;
    }

    @Override
    public Response get(String name) {
        Optional<ConfigurationVO> configurationVO = configurationService.findByName(name);
        if (configurationVO.isPresent()) {
            return ResponseFactory.response(OK, configurationVO.get());
        }

        return ResponseFactory.response(NOT_FOUND);
    }

    @Override
    public Response updateProperty(String name, ConfigurationUpdate configurationUpdate) {
        hiveValidator.validate(configurationUpdate);
        
        return ResponseFactory.response(OK, configurationService.save(name, configurationUpdate.getValue()));
    }

    @Override
    public Response deleteProperty(String name) {
        int operationResult = configurationService.delete(name);
        if (operationResult == 0) {
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(CONFIG_NOT_FOUND, name));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }
        
        return ResponseFactory.response(NO_CONTENT);
    }

}
