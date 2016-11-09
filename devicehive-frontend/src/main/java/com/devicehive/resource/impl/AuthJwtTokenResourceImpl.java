package com.devicehive.resource.impl;

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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.service.OAuthTokenService;
import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.model.oauth.IdentityProviderEnum;
import com.devicehive.resource.AuthJwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.vo.JwtTokenVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

@Service
public class AuthJwtTokenResourceImpl implements AuthJwtTokenResource {

    @Autowired
    private OAuthTokenService tokenService;

    @Override
    public Response login(AccessKeyRequestVO request) {
        final IdentityProviderEnum identityProviderEnum = IdentityProviderEnum.forName(request.getProviderName());
        JwtTokenVO jwtToken = tokenService.createAccessKey(request, identityProviderEnum);
        return ResponseFactory.response(OK, jwtToken, JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED);
    }
}
