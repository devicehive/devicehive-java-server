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

import com.devicehive.base.AuthAbstractResourceTest;
import com.devicehive.security.jwt.JwtUserPayload;
import com.devicehive.security.jwt.JwtUserPayloadView;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.security.util.JwtSecretService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.JwtTokenVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;

@Ignore
public class JwtTokenResourceTest extends AuthAbstractResourceTest {

    @Autowired
    private JwtClientService jwtClientService;
    @Autowired
    private JwtSecretService jwtSecretService;

    @Test
    public void should_return_access_and_refresh_tokens_for_token_authorized_user() throws Exception {
        // Create payload
        Long userId = ADMIN_ID;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        JwtUserPayloadView.Builder builder = new JwtUserPayloadView.Builder();
        JwtUserPayloadView payload = builder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();

        JwtTokenVO jwtTokenVO = performRequest("/token/create", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), payload, CREATED, JwtTokenVO.class);
        Assert.assertNotNull(jwtTokenVO.getAccessToken());
        Assert.assertNotNull(jwtTokenVO.getRefreshToken());
    }

    @Test
    public void should_return_401_after_providing_refresh_token_of_unexisting_user() throws Exception {
        // Create payload
        Long userId = NON_EXISTING_USER_ID;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        JwtUserPayloadView.Builder builder = new JwtUserPayloadView.Builder();
        JwtUserPayloadView payload = builder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();
        // Generate refresh token
        String refreshToken = jwtClientService.generateJwtRefreshToken(payload.convertTo(), true);
        JwtTokenVO tokenVO = new JwtTokenVO();
        tokenVO.setRefreshToken(refreshToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), tokenVO, UNAUTHORIZED, JwtTokenVO.class);
        Assert.assertNull(jwtToken.getAccessToken());
    }

    @Test
    public void should_return_401_after_providing_refresh_token_of_inactive_user() throws Exception {
        // Create payload
        Long userId = INACTIVE_USER_ID;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        JwtUserPayloadView.Builder builder = new JwtUserPayloadView.Builder();
        JwtUserPayloadView payload = builder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();

        JwtTokenVO token = new JwtTokenVO();
        String refreshToken = jwtClientService.generateJwtRefreshToken(payload.convertTo(), true);
        token.setRefreshToken(refreshToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), token, UNAUTHORIZED, JwtTokenVO.class);
        Assert.assertNull(jwtToken.getAccessToken());
    }

    @Test
    public void should_return_400_after_providing_invalid_refresh_token() throws Exception {
        // Create payload
        Long userId = ADMIN_ID;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        JwtUserPayloadView.Builder builder = new JwtUserPayloadView.Builder();
        JwtUserPayloadView payload = builder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();

        // Generate token with access type instead of refresh
        String accessToken = jwtClientService.generateJwtAccessToken(payload.convertTo(), true);
        JwtTokenVO tokenVO = new JwtTokenVO();
        tokenVO.setRefreshToken(accessToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), tokenVO, UNAUTHORIZED, JwtTokenVO.class);
        Assert.assertNull(jwtToken.getAccessToken());
    }

    @Test
    public void should_return_401_after_providing_expired_refresh_token() throws Exception {
        // Create payload
        Long userId = ADMIN_ID;
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        JwtUserPayloadView.Builder builder = new JwtUserPayloadView.Builder();
        JwtUserPayloadView payload = builder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();

        // Generate expired refresh token
        payload.setExpiration(new Date(System.currentTimeMillis() - 100));
        payload.setTokenType(TokenType.REFRESH);
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtUserPayload.JWT_CLAIM_KEY, payload.convertTo());
        Claims claims = Jwts.claims(jwtMap);
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, jwtSecretService.getJwtSecret())
                .compact();

        JwtTokenVO tokenVO = new JwtTokenVO();
        tokenVO.setRefreshToken(refreshToken);

        JwtTokenVO jwtToken = performRequest("/token/refresh", "POST", emptyMap(), emptyMap(), tokenVO, UNAUTHORIZED, JwtTokenVO.class);
        Assert.assertNull(jwtToken.getAccessToken());
    }
}
