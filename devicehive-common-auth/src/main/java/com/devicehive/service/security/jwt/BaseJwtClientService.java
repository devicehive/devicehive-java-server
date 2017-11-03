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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * Class responsible for access and refresh JWT keys generation.
 */
@Component
public class BaseJwtClientService {

    private final JwtSecretService jwtSecretService;

    @Autowired
    public BaseJwtClientService(JwtSecretService jwtSecretService) {
        this.jwtSecretService = jwtSecretService;
    }

    @Cacheable("payload")
    @SuppressWarnings("unchecked")
    public JwtPayload getPayload(String jwtToken) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecretService.getJwtSecret())
                .parseClaimsJws(jwtToken)
                .getBody();
        LinkedHashMap<String, Object> payloadMap = (LinkedHashMap<String, Object>) claims.get(JwtPayload.JWT_CLAIM_KEY);

        JwtPayload.Builder builder = new JwtPayload.Builder();

        Optional.ofNullable(payloadMap.get(JwtPayload.USER_ID))
                .ifPresent(userId -> builder.withUserId(Long.valueOf(userId.toString())));
        Optional.ofNullable((ArrayList<String>) payloadMap.get(JwtPayload.NETWORK_IDS))
                .ifPresent(networkIds -> builder.withNetworkIds(new HashSet<>(networkIds)));
        Optional.ofNullable((ArrayList<Integer>) payloadMap.get(JwtPayload.ACTIONS))
                .ifPresent(actions -> builder.withActions(new HashSet<>(actions)));
        Optional.ofNullable((ArrayList<String>) payloadMap.get(JwtPayload.DEVICE_IDS))
                .ifPresent(deviceIds -> builder.withDeviceIds(new HashSet<>(deviceIds)));
        Optional<Long> expiration = Optional.ofNullable((Long)payloadMap.get(JwtPayload.EXPIRATION));
        Optional<Integer> tokenType = Optional.ofNullable((Integer) payloadMap.get(JwtPayload.TOKEN_TYPE));

        if (!tokenType.isPresent() && !expiration.isPresent()) {
            throw new MalformedJwtException("Token type and expiration date should be provided in the token");
        } else {
            if (tokenType.isPresent())
                builder.withTokenType(tokenType.get());
            else
                throw new MalformedJwtException("Token type should be provided in the token");
            if (expiration.isPresent())
                builder.withExpirationDate(new Date(expiration.get()));
            else
                throw new MalformedJwtException("Expiration date should be provided in the token");
            return builder.buildPayload();
        }
    }

}
