package com.devicehive.resource.impl;

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
        return ResponseFactory.response(OK, jwtToken, JsonPolicyDef.Policy.JWT_TOKEN_SUBMITTED);
    }
}
