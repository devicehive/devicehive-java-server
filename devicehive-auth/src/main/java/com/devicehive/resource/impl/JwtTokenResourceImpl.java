package com.devicehive.resource.impl;

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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.JwtUserPayload;
import com.devicehive.security.jwt.JwtUserPayloadView;
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.BaseUserService;
import com.devicehive.service.PluginService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.security.jwt.JwtTokenService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.JwtRefreshTokenVO;
import com.devicehive.vo.JwtRequestVO;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.PluginVO;
import com.devicehive.vo.UserVO;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.ws.rs.core.Response;

import java.util.Objects;
import java.util.Optional;

import static com.devicehive.configuration.Messages.CAN_NOT_GET_CURRENT_USER;
import static com.devicehive.configuration.Messages.EMPTY_TOKEN;
import static com.devicehive.configuration.Messages.EXPIRED_TOKEN;
import static com.devicehive.configuration.Messages.INVALID_TOKEN;
import static com.devicehive.configuration.Messages.INVALID_TOKEN_TYPE;
import static com.devicehive.configuration.Messages.INVALID_TOPIC_NAME;
import static com.devicehive.configuration.Messages.PLUGIN_NOT_ACTIVE;
import static com.devicehive.configuration.Messages.PLUGIN_NOT_FOUND;
import static com.devicehive.configuration.Messages.USER_NOT_ACTIVE;
import static com.devicehive.configuration.Messages.USER_NOT_FOUND;
import static com.devicehive.configuration.Messages.USER_NOT_PLUGIN_CREATOR;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Service
public class JwtTokenResourceImpl implements JwtTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenResourceImpl.class);

    private final JwtClientService tokenService;
    private final BaseUserService userService;
    private final TimestampService timestampService;
    private final JwtTokenService jwtTokenService;
    private final HiveValidator hiveValidator;
    private final PluginService pluginService;

    @Autowired
    public JwtTokenResourceImpl(JwtClientService tokenService,
                                BaseUserService userService,
                                TimestampService timestampService,
                                JwtTokenService jwtTokenService,
                                HiveValidator hiveValidator,
                                PluginService pluginService) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.timestampService = timestampService;
        this.jwtTokenService = jwtTokenService;
        this.hiveValidator = hiveValidator;
        this.pluginService = pluginService;
    }

    @Override
    public Response tokenRequest(JwtUserPayloadView payloadView) {
        JwtUserPayload payload = payloadView.convertTo();
        hiveValidator.validate(payload);
        JwtTokenVO responseTokenVO = new JwtTokenVO();

        UserVO user = userService.findById(payload.getUserId());
        if (user == null) {
            logger.warn(String.format(USER_NOT_FOUND, payload.getUserId()));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(USER_NOT_FOUND, payload.getUserId())));
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            logger.warn("JwtToken: User with specified id {} is not active", payload.getUserId());
            return ResponseFactory.response(FORBIDDEN, new ErrorResponse(FORBIDDEN.getStatusCode(), USER_NOT_ACTIVE));
        }

        logger.debug("JwtToken: generate access and refresh token");

        JwtUserPayload.JwtUserPayloadBuilder refreshPayload = JwtUserPayload.newBuilder().withPayload(payload);
        if (payloadView.getRefreshExpiration() != null) {
            refreshPayload.withExpirationDate(payloadView.getRefreshExpiration());
        }

        responseTokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload, true));
        responseTokenVO.setRefreshToken(tokenService.generateJwtRefreshToken(refreshPayload.buildPayload(), true));

        return ResponseFactory.response(CREATED, responseTokenVO, JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED);
    }

    @Override
    public Response refreshTokenRequest(JwtRefreshTokenVO requestTokenVO) {
        hiveValidator.validate(requestTokenVO);
        JwtPayload payload;

        try {
            payload = tokenService.getPayload(requestTokenVO.getRefreshToken());
        } catch (JwtException e) {
            logger.error(e.getMessage());
            return ResponseFactory.response(UNAUTHORIZED);
        }

        if (!payload.getTokenType().equals(TokenType.REFRESH.getId())) {
            logger.warn("JwtToken: refresh token is not valid");
            return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(),
                    INVALID_TOKEN_TYPE));
        }
        if (payload.getExpiration().before(timestampService.getDate())) {
            logger.warn("JwtToken: refresh token has expired");
            return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(),
                    EXPIRED_TOKEN));
        }

        return payload.isUserPayload() ? getRefreshResponse((JwtUserPayload) payload) : 
                getRefreshResponse((JwtPluginPayload) payload);
    }

    private Response getRefreshResponse(JwtUserPayload payload) {
        UserVO user = userService.findById(payload.getUserId());
        if (user == null) {
            logger.warn("JwtToken: User not found");
            return ResponseFactory.response(UNAUTHORIZED);
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            logger.warn("JwtToken: User is not active");
            return ResponseFactory.response(UNAUTHORIZED);
        }


        JwtTokenVO responseTokenVO = new JwtTokenVO();
        responseTokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload, false));
        userService.refreshUserLoginData(user);
        logger.debug("JwtToken: access token successfully generated with refresh token");
        return ResponseFactory.response(CREATED, responseTokenVO, JsonPolicyDef.Policy.JWT_ACCESS_TOKEN_SUBMITTED);
    }

    private Response getRefreshResponse(JwtPluginPayload payload) {
        String topic = payload.getTopic();
        if (StringUtils.isEmpty(topic)) {
            logger.warn(INVALID_TOPIC_NAME);
            return ResponseFactory.response(UNAUTHORIZED,
                    new ErrorResponse(UNAUTHORIZED.getStatusCode(), INVALID_TOPIC_NAME));
        }

        PluginVO pluginVO = pluginService.findByTopic(topic);
        if (pluginVO == null) {
            logger.warn(PLUGIN_NOT_FOUND);
            return ResponseFactory.response(UNAUTHORIZED,
                    new ErrorResponse(UNAUTHORIZED.getStatusCode(), PLUGIN_NOT_FOUND));
        }

        JwtTokenVO responseTokenVO = new JwtTokenVO();
        responseTokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload, false));
        logger.debug("JwtToken: plugin access token successfully generated with refresh token");
        return ResponseFactory.response(CREATED, responseTokenVO, JsonPolicyDef.Policy.JWT_ACCESS_TOKEN_SUBMITTED);
    }

    @Override
    public Response login(JwtRequestVO request) {
        JwtTokenVO jwtToken = jwtTokenService.createJwtToken(request);
        return ResponseFactory.response(CREATED, jwtToken, JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED);
    }

    @Override
    public Response authenticatePlugin(String jwtPluginToken) {
        if (StringUtils.isEmpty(jwtPluginToken)) {
            logger.warn(EMPTY_TOKEN);
            return ResponseFactory.response(UNAUTHORIZED,
                    new ErrorResponse(UNAUTHORIZED.getStatusCode(), EMPTY_TOKEN));
        }

        JwtPluginPayload jwtPluginPayload = null;
        try {
            jwtPluginPayload = tokenService.getPluginPayload(jwtPluginToken);
        } catch (Exception e) {
            logger.warn(INVALID_TOKEN);
            return ResponseFactory.response(UNAUTHORIZED, 
                    new ErrorResponse(UNAUTHORIZED.getStatusCode(), INVALID_TOKEN));
        }

        if (jwtPluginPayload == null ||
                jwtPluginPayload.getTokenType().equals(TokenType.REFRESH.getId())) {
            logger.warn(INVALID_TOKEN_TYPE);
            return ResponseFactory.response(UNAUTHORIZED,
                    new ErrorResponse(UNAUTHORIZED.getStatusCode(), INVALID_TOKEN_TYPE));
        }
        if (jwtPluginPayload.getExpiration() != null && jwtPluginPayload.getExpiration().before(timestampService.getDate())) {
            logger.warn(EXPIRED_TOKEN);
            return ResponseFactory.response(UNAUTHORIZED, 
                    new ErrorResponse(UNAUTHORIZED.getStatusCode(), EXPIRED_TOKEN));
        }
        
        if (jwtPluginPayload.getTopic() != null) {
            PluginVO pluginVO = pluginService.findByTopic(jwtPluginPayload.getTopic());
            if (pluginVO == null) {
                logger.warn(PLUGIN_NOT_FOUND);
                return ResponseFactory.response(UNAUTHORIZED, 
                        new ErrorResponse(UNAUTHORIZED.getStatusCode(), PLUGIN_NOT_FOUND));
            }
        }
        
        return ResponseFactory.response(OK, jwtPluginPayload);
    }

    @Override
    public Response pluginTokenRequest(JwtPluginPayload payload) {
        hiveValidator.validate(payload);
        JwtTokenVO responseTokenVO = new JwtTokenVO();

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO user = principal.getUser();
        if (user == null) {
            logger.warn(CAN_NOT_GET_CURRENT_USER);
            return ResponseFactory.response(CONFLICT,
                    new ErrorResponse(CONFLICT.getStatusCode(), CAN_NOT_GET_CURRENT_USER));
        }

        Long creatorId = Optional.ofNullable(payload.getTopic())
                .map(topic -> getCreatorId(topic, user))
                .orElse(null);
        
        if (Objects.isNull(creatorId)) {
            logger.warn(INVALID_TOPIC_NAME);
            return ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(FORBIDDEN.getStatusCode(), INVALID_TOPIC_NAME));
        }
        
        if (!user.getId().equals(creatorId)) {
            logger.warn(String.format(USER_NOT_PLUGIN_CREATOR, creatorId));
            return ResponseFactory.response(FORBIDDEN, 
                    new ErrorResponse(FORBIDDEN.getStatusCode(), String.format(USER_NOT_PLUGIN_CREATOR, creatorId)));
        }
        
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            logger.warn(USER_NOT_ACTIVE);
            return ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(FORBIDDEN.getStatusCode(), USER_NOT_ACTIVE));
        }

        logger.debug("JwtToken: generate access and refresh token");

        JwtPluginPayload refreshPayload = JwtPluginPayload.newBuilder().withPayload(payload)
                .buildPayload();

        responseTokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload, true));
        responseTokenVO.setRefreshToken(tokenService.generateJwtRefreshToken(refreshPayload, true));

        return ResponseFactory.response(CREATED, responseTokenVO, JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED);
    }

    private Long getCreatorId(String topic, UserVO user) {
        if (topic.equals("*")) {
            return user.isAdmin() ? user.getId() : null;
        }
        
        PluginVO pluginVO = pluginService.findByTopic(topic);

        return Optional.ofNullable(pluginVO)
                .map(plugin -> plugin.getUserId())
                .orElse(null);
    }

}
