package com.devicehive.model.oauth;

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

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by tmatvienko on 1/9/15.
 */
public enum IdentityProviderEnum {
    GOOGLE("Google"), FACEBOOK("Facebook"), GITHUB("Github"), PASSWORD("Password");

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderEnum.class);

    private String value;

    IdentityProviderEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static IdentityProviderEnum forName(String value) {
        if (StringUtils.isBlank(value)) {
            logger.error(String.format(Messages.INVALID_REQUEST_PARAMETERS, value));
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, HttpServletResponse.SC_BAD_REQUEST);
        }
        for (IdentityProviderEnum type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        logger.error(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, value));
        throw new HiveException("Illegal provider name was found: " + value, HttpServletResponse.SC_UNAUTHORIZED);
    }
}
