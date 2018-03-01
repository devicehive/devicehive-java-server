package com.devicehive.security.util;

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

import com.devicehive.configuration.Constants;
import com.devicehive.service.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.SecureRandom;

@Component
public class JwtSecretService {

    private final ConfigurationService configurationService;

    private String secret;

    @Autowired
    public JwtSecretService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void init() {
    	secret = System.getenv(Constants.ENV_SECRET_VAR_NAME);
        if (!StringUtils.isEmpty(secret)) {
        	configurationService.save(Constants.DB_SECRET_VAR_NAME, secret);
        	return;
        }
        
        secret = configurationService.get(Constants.DB_SECRET_VAR_NAME);
        if (StringUtils.isEmpty(secret)) {
        	secret = new BigInteger(130, new SecureRandom()).toString(32);
        	configurationService.save(Constants.DB_SECRET_VAR_NAME, secret);
        }
    }

    public String getJwtSecret() {
        return secret;
    }

}
