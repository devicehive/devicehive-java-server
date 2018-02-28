package com.devicehive.security.util;

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

import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.JwtUserPayload;
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.time.TimestampService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to generate plugin token.
 */
@Component
public class JwtPluginTokenGenerator {

    private final TimestampService timestampService;
    private final JwtSecretService jwtSecretService;

    @Value("${jwt.refresh-token-max-age}")
    long refreshTokenMaxAge;

    @Value("${jwt.access-token-max-age}")
    long accessTokenMaxAge;

    @Autowired
    public JwtPluginTokenGenerator(TimestampService timestampService, JwtSecretService jwtSecretService) {
        this.timestampService = timestampService;
        this.jwtSecretService = jwtSecretService;
    }

    /**
     * Generates a JWT plugin token containing all needed claims. These properties are taken from the specified
     * JwtUserPayload object.
     *
     * @param payload the payload entity with which the token will be generated
     * @return the JWT plugin token
     */
    public String generateToken(JwtPluginPayload payload, TokenType tokenType, boolean useExpiration) {
        long maxAge = tokenType.equals(TokenType.ACCESS) ? accessTokenMaxAge : refreshTokenMaxAge;
        Date expiration = useExpiration && payload.getExpiration() != null ? payload.getExpiration() :
                timestampService.getDate(System.currentTimeMillis() + maxAge);

        JwtPluginPayload generatedPayload = (JwtPluginPayload) JwtPluginPayload.newBuilder()
                .withPayload(payload)
                .withExpirationDate(expiration)
                .withTokenType(tokenType.getId())
                .buildPayload();
        
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtUserPayload.JWT_CLAIM_KEY, generatedPayload);

        Claims claims = Jwts.claims(jwtMap);
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, jwtSecretService.getJwtSecret())
                .compact();
    }

}
