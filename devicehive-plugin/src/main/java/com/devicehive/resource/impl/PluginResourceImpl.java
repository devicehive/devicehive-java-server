package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.model.query.PluginUpdateQuery;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.resource.PluginResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.PluginRegisterService;
import com.devicehive.service.PluginService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.PluginVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import java.util.Collections;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.configuration.Messages.HEALTH_CHECK_FAILED;
import static com.devicehive.configuration.Messages.NO_ACCESS_TO_PLUGIN;
import static com.devicehive.configuration.Messages.PLUGIN_NOT_FOUND;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class PluginResourceImpl implements PluginResource {

    private static final Logger logger = LoggerFactory.getLogger(PluginResourceImpl.class);

    private final HiveValidator hiveValidator;
    private final PluginRegisterService pluginRegisterService;
    private final PluginService pluginService;

    @Autowired
    public PluginResourceImpl(HiveValidator hiveValidator, PluginRegisterService pluginRegisterService, PluginService pluginService) {
        this.hiveValidator = hiveValidator;
        this.pluginRegisterService = pluginRegisterService;
        this.pluginService = pluginService;
    }

    @Override
    public void list(String name, String namePattern, String topicName, Integer status, Long userId, String sortField,
                     String sortOrderSt, Integer take, Integer skip, AsyncResponse asyncResponse) {
        logger.debug("Plugin list requested");

        if (sortField != null
                && !NAME.equalsIgnoreCase(sortField)
                && !ID.equalsIgnoreCase(sortField)) {
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO user = principal.getUser();

        if (!user.isAdmin() && userId != null && !userId.equals(user.getId())) {
            logger.warn(Messages.NO_ACCESS_TO_PLUGIN);
            final Response response = ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(FORBIDDEN.getStatusCode(), Messages.NO_ACCESS_TO_PLUGIN));
            asyncResponse.resume(response);
        } else {
            pluginRegisterService.list(name, namePattern, topicName, status, userId, sortField, sortOrderSt, take, skip, principal)
                    .thenApply(plugins -> {
                        logger.debug("Plugin list request proceed successfully");
                        return ResponseFactory.response(OK, plugins, JsonPolicyDef.Policy.PLUGINS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void count(String name, String namePattern, String topicName, Integer status, Long userId, AsyncResponse asyncResponse) {
        logger.debug("Plugin count requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO user = principal.getUser();

        if (!user.isAdmin() && userId != null && !userId.equals(user.getId())) {
            logger.warn(Messages.NO_ACCESS_TO_PLUGIN);
            final Response response = ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(FORBIDDEN.getStatusCode(), Messages.NO_ACCESS_TO_PLUGIN));
            asyncResponse.resume(response);
        } else {
            pluginRegisterService.count(name, namePattern, topicName, status, userId, principal)
                    .thenApply(count -> {
                        logger.debug("Plugin count request proceed successfully");
                        return ResponseFactory.response(OK, count, JsonPolicyDef.Policy.PLUGINS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void register(PluginReqisterQuery pluginReqisterQuery, PluginUpdate pluginUpdate, String authorization,
            @Suspended final AsyncResponse asyncResponse) {
        hiveValidator.validate(pluginUpdate);
        try {
            HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            pluginRegisterService.register(principal.getUser().getId(), pluginReqisterQuery, pluginUpdate, authorization)
                    .thenAccept(asyncResponse::resume);
        } catch (ServiceUnavailableException e) {
            logger.warn(HEALTH_CHECK_FAILED);
            asyncResponse.resume(ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), HEALTH_CHECK_FAILED)));
        }
    }

    @Override
    public void update(String topicName, PluginUpdateQuery updateQuery, String authorization, AsyncResponse asyncResponse) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO user = principal.getUser();

        PluginVO pluginVO = getPluginVO(topicName, asyncResponse, principal, user);

        if (!asyncResponse.isDone()) {
            pluginRegisterService.update(pluginVO, updateQuery)
                    .thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void delete(String topicName, String authorization, AsyncResponse asyncResponse) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO user = principal.getUser();

        PluginVO pluginVO = getPluginVO(topicName, asyncResponse, principal, user);

        if (!asyncResponse.isDone()) {
            pluginRegisterService.delete(pluginVO)
                    .thenAccept(asyncResponse::resume);
        }
    }

    private PluginVO getPluginVO(String topicName, AsyncResponse asyncResponse, HivePrincipal principal, UserVO user) {
        PluginVO pluginVO;
        if (principal.getPlugin() != null) {
            pluginVO = principal.getPlugin();
        } else {
            pluginVO = pluginService.findByTopic(topicName);
            if (pluginVO == null) {
                if (user.isAdmin()) {
                    asyncResponse.resume(ResponseFactory.response(NOT_FOUND,
                            new ErrorResponse(NOT_FOUND.getStatusCode(), PLUGIN_NOT_FOUND)));
                } else {
                    asyncResponse.resume(ResponseFactory.response(FORBIDDEN,
                            new ErrorResponse(FORBIDDEN.getStatusCode(), NO_ACCESS_TO_PLUGIN)));
                }
            } else if (!user.isAdmin() && pluginVO.getUserId() != null && !pluginVO.getUserId().equals(user.getId())) {
                asyncResponse.resume(ResponseFactory.response(FORBIDDEN,
                        new ErrorResponse(FORBIDDEN.getStatusCode(), NO_ACCESS_TO_PLUGIN)));
            }
        }
        return pluginVO;
    }
}
