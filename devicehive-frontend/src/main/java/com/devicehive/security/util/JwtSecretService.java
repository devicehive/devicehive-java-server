package com.devicehive.security.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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


import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.devicehive.service.configuration.ConfigurationService;

@Component
public class JwtSecretService {

	private final String SECRET_VAR_NAME = "JWT_SECRET";
    private String secret;
    
    @Autowired
    private ConfigurationService configurationService;
    
    @PostConstruct
    public void init() {
    	secret = System.getenv(SECRET_VAR_NAME);
        if (!StringUtils.isEmpty(secret)) {
        	configurationService.save(SECRET_VAR_NAME, secret);
        	return;
        }
        
        secret = configurationService.get(SECRET_VAR_NAME);
        if (StringUtils.isEmpty(secret)) {
        	secret = new BigInteger(130, new SecureRandom()).toString(32);
        	configurationService.save(SECRET_VAR_NAME, secret);
        }
    }

    public String getJwtSecret() {
        return secret;
    }

}
