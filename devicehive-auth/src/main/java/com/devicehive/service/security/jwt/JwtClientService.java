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
public class JwtClientService extends BaseJwtClientService {

    private final JwtTokenGenerator tokenGenerator;
    
    @Autowired
    public JwtClientService(JwtTokenGenerator tokenGenerator, JwtSecretService jwtSecretService) {
        super(jwtSecretService);
        this.tokenGenerator = tokenGenerator;
    }

    public String generateJwtAccessToken(JwtPayload payload, boolean useExpiration) {
        return tokenGenerator.generateToken(payload, TokenType.ACCESS, useExpiration);
    }

    public String generateJwtRefreshToken(JwtPayload payload, boolean useExpiration) {
        return tokenGenerator.generateToken(payload, TokenType.REFRESH, useExpiration);
    }


}
