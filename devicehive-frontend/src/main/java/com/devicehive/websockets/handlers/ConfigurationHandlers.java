package com.devicehive.websockets.handlers;

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

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.vo.ConfigurationVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.Optional;

import static com.devicehive.configuration.Constants.CONFIGURATION;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.configuration.Constants.VALUE;
import static com.devicehive.configuration.Messages.CONFIG_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Component
public class ConfigurationHandlers {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationHandlers.class);

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private Gson gson;

    @Value("${server.context-path}")
    private String contextPath;

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_CONFIGURATION')")
    public WebSocketResponse processConfigurationGet(JsonObject request, WebSocketSession session) {
        final String name = gson.fromJson(request.get(NAME), String.class);
        if (Objects.isNull(name)) {
            logger.error("congiguration/get proceed with error. Name should be provided.");
            throw new HiveException(Messages.CONFIGURATION_NAME_REQUIRED, SC_BAD_REQUEST);
        }
        
        Optional<ConfigurationVO> configurationVO = configurationService.findByName(name);
        if (!configurationVO.isPresent()) {
            logger.error(String.format(CONFIG_NOT_FOUND, name));
            throw new HiveException(String.format(CONFIG_NOT_FOUND, name), SC_NOT_FOUND);
        }
        
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(CONFIGURATION, configurationVO.get());
        return response;
        
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_CONFIGURATION')")
    public WebSocketResponse processConfigurationPut(JsonObject request, WebSocketSession session) {
        final String name = gson.fromJson(request.get(NAME), String.class);
        if (Objects.isNull(name)) {
            logger.error("congiguration/put proceed with error. Name should be provided.");
            throw new HiveException(Messages.CONFIGURATION_NAME_REQUIRED, SC_BAD_REQUEST);
        }
        
        final String value = gson.fromJson(request.get(VALUE), String.class);
        
        ConfigurationVO configurationVO = configurationService.save(name, value);
        
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(CONFIGURATION, configurationVO);
        return response;
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_CONFIGURATION')")
    public WebSocketResponse processConfigurationDelete(JsonObject request, WebSocketSession session) {
        final String name = gson.fromJson(request.get(NAME), String.class);
        if (Objects.isNull(name)) {
            logger.error("congiguration/delete proceed with error. Name should be provided.");
            throw new HiveException(Messages.CONFIGURATION_NAME_REQUIRED, SC_BAD_REQUEST);
        }
        
        int operationResult = configurationService.delete(name);
        if (operationResult == 0) {
            throw new HiveException(String.format(CONFIG_NOT_FOUND, name), SC_NOT_FOUND);
        }
        
        return new WebSocketResponse();
    }
}
