package com.devicehive.resource.impl;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.OAuthClientService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.JwtTokenVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CREATED;

@Service
public class JwtTokenResourceImpl implements JwtTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenResourceImpl.class);

    @Autowired
    private JwtClientService tokenService;

    @Autowired
    private AccessKeyService accessKeyService;

    @Autowired
    private UserService userService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private OAuthClientService clientService;

    @Override
    public Response tokenRequest(JwtPayload payload) {
        JwtTokenVO tokenVO = new JwtTokenVO();

        logger.debug("JwtToken: requesting access by refresh token");

        tokenVO.setToken(tokenService.generateJwtAccessToken(payload));

        return ResponseFactory.response(CREATED, tokenVO, JsonPolicyDef.Policy.JWT_TOKEN_SUBMITTED);
    }
}
