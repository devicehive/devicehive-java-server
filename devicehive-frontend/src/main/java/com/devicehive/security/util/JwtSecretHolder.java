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


import java.util.UUID;

public class JwtSecretHolder {

    private String secret = System.getenv("JWT_SECRET");

    public static final JwtSecretHolder INSTANCE = new JwtSecretHolder();

    private JwtSecretHolder() {
        if (secret == null) {
            secret = UUID.randomUUID().toString();
        }
    }

    public String getJwtSecret() {
        return secret;
    }

}
