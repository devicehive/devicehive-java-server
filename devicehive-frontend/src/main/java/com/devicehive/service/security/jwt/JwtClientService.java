package com.devicehive.service.security.jwt;

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
import com.devicehive.security.util.JwtSecretService;
import com.devicehive.security.util.JwtTokenGenerator;
import com.hazelcast.util.MapUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Class responsible for access and refresh JWT keys generation.
 */
@Component
public class JwtClientService {

    private final JwtTokenGenerator tokenGenerator;
    private final JwtSecretService jwtSecretService;

    @Autowired
    public JwtClientService(JwtTokenGenerator tokenGenerator, JwtSecretService jwtSecretService) {
        this.tokenGenerator = tokenGenerator;
        this.jwtSecretService = jwtSecretService;
    }

    public String generateJwtAccessToken(JwtPayload payload, boolean useExpiration) {
        return tokenGenerator.generateToken(payload, TokenType.ACCESS, useExpiration);
    }

    public String generateJwtRefreshToken(JwtPayload payload, boolean useExpiration) {
        return tokenGenerator.generateToken(payload, TokenType.REFRESH, useExpiration);
    }

    @Cacheable("payload")
    public JwtPayload getPayload(String jwtToken) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecretService.getJwtSecret())
                .parseClaimsJws(jwtToken)
                .getBody();
        LinkedHashMap payloadMap = (LinkedHashMap) claims.get(JwtPayload.JWT_CLAIM_KEY);

        Optional userId = Optional.ofNullable(payloadMap.get(JwtPayload.USER_ID));
        Optional networkIds = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.NETWORK_IDS));
        Optional actions = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.ACTIONS));
        Optional deviceIds = Optional.ofNullable((ArrayList) payloadMap.get(JwtPayload.DEVICE_IDS));
        Optional expiration = Optional.ofNullable(payloadMap.get(JwtPayload.EXPIRATION));
        Optional<Integer> tokenType = Optional.ofNullable((Integer) payloadMap.get(JwtPayload.TOKEN_TYPE));

        JwtPayload.Builder builder = new JwtPayload.Builder();
        if (userId.isPresent()) builder.withUserId(Long.valueOf(userId.get().toString()));
        if (networkIds.isPresent()) builder.withNetworkIds(new HashSet<>((ArrayList) networkIds.get()));
        if (actions.isPresent()) builder.withActions(new HashSet<>((ArrayList) actions.get()));
        if (deviceIds.isPresent()) builder.withDeviceIds(new HashSet<>((ArrayList) deviceIds.get()));
        if (!tokenType.isPresent() && !expiration.isPresent()) {
            throw new MalformedJwtException("Token type and expiration date should be provided in the token");
        } else {
            if (tokenType.isPresent())
                builder.withTokenType(tokenType.get());
            else
                throw new MalformedJwtException("Token type should be provided in the token");
            if (expiration.isPresent())
                builder.withExpirationDate(new Date((Long)expiration.get()));
            else
                throw new MalformedJwtException("Expiration date should be provided in the token");
            return builder.buildPayload();
        }
    }

}
