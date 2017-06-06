package com.devicehive.resource;

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

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.security.util.JwtSecretService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JwtTokenResourceTest extends AbstractResourceTest {

    @Autowired
    private JwtClientService jwtClientService;
    @Autowired
    private JwtSecretService jwtSecretService;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_access_and_refresh_tokens_for_token_authorized_user() throws Exception {
        // Create test user
        UserUpdate testUser = new UserUpdate();
        testUser.setLogin("string_1");
        testUser.setRole(UserRole.CLIENT.getValue());
        testUser.setPassword("string_1");
        testUser.setStatus(UserStatus.ACTIVE.getValue());

        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, CREATED, UserVO.class);
        final long userId = user.getId();

        // Create payload
//        Long userId = RandomUtils.nextLong(10, 1000);
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds, deviceIds).buildPayload();

        JwtTokenVO jwtTokenVO = performRequest("/token/create", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), payload, CREATED, JwtTokenVO.class);
        assertNotNull(jwtTokenVO.getAccessToken());
        assertNotNull(jwtTokenVO.getRefreshToken());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_401_after_providing_refresh_token_of_unexisting_user() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000);
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds, deviceIds).buildPayload();
        // Generate refresh token
        String refreshToken = jwtClientService.generateJwtRefreshToken(payload);
        JwtTokenVO tokenVO = new JwtTokenVO();
        tokenVO.setRefreshToken(refreshToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), tokenVO, UNAUTHORIZED, JwtTokenVO.class);
        assertNull(jwtToken.getAccessToken());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_401_after_providing_refresh_token_of_inactive_user() throws Exception {
        // Create test user
        UserUpdate testUser = new UserUpdate();
        testUser.setLogin("string_1");
        testUser.setRole(UserRole.CLIENT.getValue());
        testUser.setPassword("string_1");
        testUser.setStatus(UserStatus.DISABLED.getValue());

        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, CREATED, UserVO.class);
        final long userid = user.getId();
        // Create payload
        Long userId = userid;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds, deviceIds).buildPayload();

        JwtTokenVO token = new JwtTokenVO();
        String refreshToken = jwtClientService.generateJwtAccessToken(payload);
        token.setRefreshToken(refreshToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), token, UNAUTHORIZED, JwtTokenVO.class);
        assertNull(jwtToken.getAccessToken());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_400_after_providing_invalid_refresh_token() throws Exception {
        // Create test user
        UserUpdate testUser = new UserUpdate();
        testUser.setLogin("string_1");
        testUser.setRole(UserRole.CLIENT.getValue());
        testUser.setPassword("string_1");
        testUser.setStatus(UserStatus.ACTIVE.getValue());

        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, CREATED, UserVO.class);
        final long userid = user.getId();
        // Create payload
        Long userId = userid;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds, deviceIds).buildPayload();

        // Generate token with access type instead of refresh
        String accessToken = jwtClientService.generateJwtAccessToken(payload);
        JwtTokenVO tokenVO = new JwtTokenVO();
        tokenVO.setRefreshToken(accessToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), tokenVO, BAD_REQUEST, JwtTokenVO.class);
        assertNull(jwtToken.getAccessToken());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_401_after_providing_expired_refresh_token() throws Exception {
        // Create test user
        UserUpdate testUser = new UserUpdate();
        testUser.setLogin("string_1");
        testUser.setRole(UserRole.CLIENT.getValue());
        testUser.setPassword("string_1");
        testUser.setStatus(UserStatus.ACTIVE.getValue());

        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, CREATED, UserVO.class);
        // Create payload
        Long userId = user.getId();
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds, deviceIds).buildPayload();

        // Generate expired refresh token
        payload.setExpiration(new Date(System.currentTimeMillis() - 100));
        payload.setTokenType(TokenType.REFRESH);
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtPayload.JWT_CLAIM_KEY, payload);
        Claims claims = Jwts.claims(jwtMap);
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, jwtSecretService.getJwtSecret())
                .compact();

        JwtTokenVO tokenVO = new JwtTokenVO();
        tokenVO.setRefreshToken(refreshToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), tokenVO, UNAUTHORIZED, JwtTokenVO.class);
        assertNull(jwtToken.getAccessToken());
    }
}
