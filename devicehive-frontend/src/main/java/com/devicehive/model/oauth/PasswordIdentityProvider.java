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
import com.devicehive.service.OAuthTokenService;
import com.devicehive.service.UserService;
import com.devicehive.vo.OauthJwtRequestVO;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

/**
 * Created by tmatvienko on 1/13/15.
 */
@Component
public class PasswordIdentityProvider extends AuthProvider {
    private static final Logger logger = LoggerFactory.getLogger(PasswordIdentityProvider.class);

    private static final String PASSWORD_PROVIDER_NAME = "Password";

    @Autowired
    private UserService userService;
    @Autowired
    private OAuthTokenService tokenService;

    @Override
    public boolean isIdentityProviderAllowed() {
        return true;
    }

    @Override
    public JwtTokenVO createAccessKey(@NotNull final OauthJwtRequestVO request) {
        if (StringUtils.isBlank(request.getLogin()) || StringUtils.isBlank(request.getPassword())) {
            logger.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
            throw new HiveException(Messages.INVALID_AUTH_REQUEST_PARAMETERS, Response.Status.BAD_REQUEST.getStatusCode());
        }
        final UserVO user = findUser(request.getLogin(), request.getPassword());
        return tokenService.authenticate(user);
    }

    private UserVO findUser(String login, String password) {
        return userService.findUser(login, password);
    }
}
