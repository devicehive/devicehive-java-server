package com.devicehive.service;

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
import com.devicehive.security.jwt.TokenType;
import com.devicehive.security.util.JwtSecretService;
import com.devicehive.service.security.jwt.JwtClientService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Ignore
public class JwtClientServiceTest  extends AuthAbstractResourceTest {
    private static final String JWT_REFRESH_WITH_INVALID_SIGNATURE = "eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7InVzZXJJZCI6MSwiYWN0aW9ucyI6WyIqIl0sIm5ldHdvcmtJZHMiOlsiKiJdLCJkZXZpY2VJZHMiOlsiKiJdLCJleHBpcmF0aW9uIjoxNTU5MzQ3MjAwMDAwLCJ0b2tlblR5cGUiOiJSRUZSRVNIIn19.kCFPLoGGoCyaHqS3Vv5tjK_d2xQcPKTsM2z4PhKP64Q";

    @Autowired
    private JwtClientService jwtClientService;
    @Autowired
    private JwtSecretService jwtSecretService;
            
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void should_generate_jwt_token_with_access_type() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000); 
        Set<Integer> actions = new HashSet<>();
        actions.add(0);
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtUserPayload.JwtUserPayloadBuilder jwtUserPayloadBuilder = new JwtUserPayload.JwtUserPayloadBuilder();
        JwtUserPayload payload = jwtUserPayloadBuilder.withPublicClaims(userId, actions, networkIds,deviceTypeIds).buildPayload();

        String token = jwtClientService.generateJwtAccessToken(payload, true);
        JwtUserPayload resultPayload = jwtClientService.getUserPayload(token);

        assertEquals(resultPayload.getTokenType(), TokenType.ACCESS.getId());
    }

    @Test
    public void should_generate_jwt_token_with_refresh_type() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000);
        Set<Integer> actions = new HashSet<>();
        actions.add(0);
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtUserPayload.JwtUserPayloadBuilder jwtUserPayloadBuilder = new JwtUserPayload.JwtUserPayloadBuilder();
        JwtUserPayload payload = jwtUserPayloadBuilder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();

        String token = jwtClientService.generateJwtRefreshToken(payload, true);
        JwtUserPayload resultPayload = jwtClientService.getUserPayload(token);

        assertEquals(resultPayload.getTokenType(), TokenType.REFRESH.getId());
    }

    @Test(expected = MalformedJwtException.class)
    public void should_throw_MalformedJwtException_whet_pass_token_without_expiration_and_type() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000);
        Set<Integer> actions = new HashSet<>();
        actions.add(0);
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceTypeIds = new HashSet<>();
        deviceTypeIds.add("string");
        Set<String> deviceIds = new HashSet<>();
        deviceIds.add("string");
        JwtUserPayload.JwtUserPayloadBuilder jwtUserPayloadBuilder = new JwtUserPayload.JwtUserPayloadBuilder();
        JwtUserPayload payload = jwtUserPayloadBuilder.withPublicClaims(userId, actions, networkIds, deviceTypeIds).buildPayload();

        // Generate key without expiration date and token type
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtUserPayload.JWT_CLAIM_KEY, payload);
        Claims claims = Jwts.claims(jwtMap);
        String malformedToken = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, jwtSecretService.getJwtSecret())
                .compact();
        jwtClientService.getUserPayload(malformedToken);
    }

    @Test(expected = SignatureException.class)
    public void should_throw_SignatureException_whet_pass_token_with_invalid_signature() throws Exception {
        jwtClientService.getUserPayload(JWT_REFRESH_WITH_INVALID_SIGNATURE);
    }
    
}
