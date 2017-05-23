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
import com.devicehive.security.jwt.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to generate a token.
 */
@Component
public class JwtTokenGenerator {

    @Value("${jwt.refresh-token-max-age}")
    long refreshTokenMaxAge;

    @Value("${jwt.access-token-max-age}")
    long accessTokenMaxAge;

    /**
     * Generates a JWT token containing all needed claims. These properties are taken from the specified
     * JwtPayload object.
     *
     * @param payload the payload entity with which the token will be generated
     * @return the JWT token
     */
    public String generateToken(JwtPayload payload, TokenType tokenType) {
        
        long maxAge = tokenType.equals(TokenType.ACCESS) ? accessTokenMaxAge : refreshTokenMaxAge;
        Date expiration = new Date(System.currentTimeMillis() + maxAge);
        
        payload.setExpiration(expiration);
        payload.setTokenType(tokenType);
        
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtPayload.JWT_CLAIM_KEY, payload);

        Claims claims = Jwts.claims(jwtMap);
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, JwtSecretHolder.INSTANCE.getJwtSecret())
                .compact();
    }
}
