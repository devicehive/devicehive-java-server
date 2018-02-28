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
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.security.jwt.JwtUserPayload;
import com.devicehive.security.util.JwtSecretService;
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

import static com.devicehive.security.jwt.JwtPayload.EXPIRATION;
import static com.devicehive.security.jwt.JwtPayload.JWT_CLAIM_KEY;
import static com.devicehive.security.jwt.JwtPayload.TOKEN_TYPE;

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

    public JwtPayload getPayload(String jwtToken) {
        final LinkedHashMap<String, Object> payloadMap = getPayloadMap(jwtToken);
        if (Optional.ofNullable(payloadMap.get(JwtUserPayload.USER_ID)).isPresent()) {
            return getUserPayload(jwtToken);
        } else if (Optional.ofNullable(payloadMap.get(JwtPluginPayload.TOPIC)).isPresent()) {
            return getPluginPayload(jwtToken);
        } else throw new IllegalArgumentException("Unknown JWT payload format");
    }

    @Cacheable("user-payload")
    @SuppressWarnings("unchecked")
    public JwtUserPayload getUserPayload(String jwtToken) {
        LinkedHashMap<String, Object> payloadMap = getPayloadMap(jwtToken);
        Long userId = Optional.ofNullable(payloadMap.get(JwtUserPayload.USER_ID))
                .map(id -> Long.valueOf(id.toString()))
                .orElseThrow(() -> new IllegalArgumentException("Not a user payload"));

        JwtUserPayload.JwtUserPayloadBuilder jwtUserPayloadBuilder = new JwtUserPayload.JwtUserPayloadBuilder()
                .withUserId(userId);
        Optional.ofNullable((ArrayList<String>) payloadMap.get(JwtUserPayload.NETWORK_IDS))
                .ifPresent(networkIds -> jwtUserPayloadBuilder.withNetworkIds(new HashSet<>(networkIds)));
        Optional.ofNullable((ArrayList<Integer>) payloadMap.get(JwtUserPayload.ACTIONS))
                .ifPresent(actions -> jwtUserPayloadBuilder.withActions(new HashSet<>(actions)));
        Optional.ofNullable((ArrayList<String>) payloadMap.get(JwtUserPayload.DEVICE_TYPE_IDS))
                .ifPresent(deviceTypeIds -> jwtUserPayloadBuilder.withDeviceTypeIds(new HashSet<>(deviceTypeIds)));

        return (JwtUserPayload) getJwtPayload(jwtUserPayloadBuilder, payloadMap);
    }

    @Cacheable("plugin-payload")
    @SuppressWarnings("unchecked")
    public JwtPluginPayload getPluginPayload(String jwtToken) {
        LinkedHashMap<String, Object> payloadMap = getPayloadMap(jwtToken);
        String topic = Optional.ofNullable((String)payloadMap.get(JwtPluginPayload.TOPIC))
                .orElseThrow(() -> new IllegalArgumentException("Not a plugin payload"));

        JwtPluginPayload.JwtPluginPayloadBuilder jwtPluginPayloadBuilder = new JwtPluginPayload.JwtPluginPayloadBuilder()
                .withTopic(topic);

        Optional.ofNullable((ArrayList<Integer>) payloadMap.get(JwtUserPayload.ACTIONS))
                .ifPresent(actions -> jwtPluginPayloadBuilder.withActions(new HashSet<>(actions)));

        return (JwtPluginPayload) getJwtPayload(jwtPluginPayloadBuilder, payloadMap);
    }

    private JwtPayload getJwtPayload(JwtPayload.JwtPayloadBuilder jwtPayloadBuilder, LinkedHashMap<String, Object> payloadMap) {
        Optional<Long> expiration = Optional.ofNullable((Long)payloadMap.get(EXPIRATION));
        Optional<Integer> tokenType = Optional.ofNullable((Integer) payloadMap.get(TOKEN_TYPE));

        if (!tokenType.isPresent() && !expiration.isPresent()) {
            throw new MalformedJwtException("Token type and expiration date should be provided in the token");
        } else {
            if (tokenType.isPresent())
                jwtPayloadBuilder.withTokenType(tokenType.get());
            else
                throw new MalformedJwtException("Token type should be provided in the token");
            if (expiration.isPresent())
                jwtPayloadBuilder.withExpirationDate(new Date(expiration.get()));
            else
                throw new MalformedJwtException("Expiration date should be provided in the token");
            return jwtPayloadBuilder.buildPayload();
        }
    }

    private LinkedHashMap<String, Object> getPayloadMap(String jwtToken) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecretService.getJwtSecret())
                .parseClaimsJws(jwtToken)
                .getBody();
        return (LinkedHashMap<String, Object>) claims.get(JWT_CLAIM_KEY);
    }

}
