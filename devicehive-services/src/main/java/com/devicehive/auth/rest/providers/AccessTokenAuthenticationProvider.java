package com.devicehive.auth.rest.providers;

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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.AccessKeyService;
import com.devicehive.vo.AccessKeyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Date;

public class AccessTokenAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenAuthenticationProvider.class);

    @Autowired
    private AccessKeyService accessKeyService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getPrincipal();

        AccessKeyVO accessKey = accessKeyService.authenticate(token);
        if (accessKey == null
                || accessKey.getUser() == null || !accessKey.getUser().getStatus().equals(UserStatus.ACTIVE)
                || (accessKey.getExpirationDate() != null && accessKey.getExpirationDate().before(new Date()))) {
            throw new BadCredentialsException("Unauthorized"); //"Wrong access key"
        }
        logger.debug("Access token authentication successful");
        return new HiveAuthentication(
                new HivePrincipal(accessKey),
                AuthorityUtils.createAuthorityList(HiveRoles.KEY));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
