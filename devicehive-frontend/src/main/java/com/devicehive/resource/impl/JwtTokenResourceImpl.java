package com.devicehive.resource.impl;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.UserVO;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

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

    @Override
    public Response tokenRequest(JwtPayload payload) {
        JwtTokenVO tokenVO = new JwtTokenVO();

        logger.debug("JwtToken: generate access and refresh token");
        tokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload));
        tokenVO.setRefreshToken(tokenService.generateJwtRefreshToken(payload));

        return ResponseFactory.response(CREATED, tokenVO, JsonPolicyDef.Policy.JWT_REFRESH_TOKEN_SUBMITTED);
    }

    @Override
    public Response refreshTokenRequest(JwtTokenVO requestTokenVO) {
        JwtTokenVO responseTokenVO = new JwtTokenVO();
        JwtPayload payload;

        try {
            payload = tokenService.getPayload(requestTokenVO.getRefreshToken());
        } catch (MalformedJwtException e) {
            logger.error(e.getMessage(), e);
            return ResponseFactory.response(UNAUTHORIZED);
        }
        
        UserVO user = userService.findById(payload.getUserId());
        if (user == null) {
            logger.warn("JwtToken: User not found");
            return ResponseFactory.response(UNAUTHORIZED);
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            logger.warn("JwtToken: User is not active");
            return ResponseFactory.response(UNAUTHORIZED);
        }
        if (!payload.getTokenType().equals(TokenType.REFRESH)) {
            logger.warn("JwtToken: refresh token is not valid");
            return ResponseFactory.response(BAD_REQUEST);
        }
        if (payload.getExpiration().before(timestampService.getDate())) {
            logger.warn("JwtToken: refresh token has expired");
            return ResponseFactory.response(UNAUTHORIZED);
        }

        responseTokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload));
        logger.debug("JwtToken: access token successfully generated with refresh token");
        return ResponseFactory.response(CREATED, responseTokenVO, JsonPolicyDef.Policy.JWT_ACCESS_TOKEN_SUBMITTED);
    }
}
