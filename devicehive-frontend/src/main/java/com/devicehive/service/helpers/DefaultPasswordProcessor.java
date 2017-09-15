package com.devicehive.service.helpers;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.configuration.Constants;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * this one uses the same scheme as .net server TODO investigate maybe it makes sense to replace it with some
 * key-stretching scheme (scrypt, PBKDF2 or bcrypt)
 */

@Component
public class DefaultPasswordProcessor implements PasswordProcessor {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSalt() {
        byte[] saltBytes = new byte[18]; // .net server uses 18 bytes salt
        secureRandom.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * TODO do we need timing-safe implementation?
     */
    @Override
    public boolean checkPassword(String password, String salt, String hash) {
        return hash.equals(hashPassword(password, salt));
    }

    /**
     * Implements self-made hash scheme.
     */
    @Override
    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest((salt + password).getBytes(Constants.UTF8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
