package com.devicehive.resource.impl;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.AccessKeyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Date;

import static javax.ws.rs.core.Response.Status.*;

@Service
public class JwtTokenResourceImpl implements JwtTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenResourceImpl.class);

    @Autowired
    private JwtClientService tokenService;

    @Autowired
    private AccessKeyService accessKeyService;

    @Override
    public Response tokenRequest(String token, String grantType, String clientCredentials) {

        switch (grantType) {
            case AUTH_HEADER:
                //TODO: go to DB and check by key if user exists
                logger.debug("JwtToken: get access key VO by key {}", token);
                AccessKeyVO accessKeyVO = accessKeyService.getAccessKey(token);
                if (accessKeyVO == null) {
                    return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                logger.debug("JwtToken: proceed successfully. AccessKey : {}", token);

                long currentTimeMillis = System.currentTimeMillis();
                Date expirationDate = new Date(currentTimeMillis + Constants.DEFAULT_JWT_REFRESH_TOKEN_MAX_AGE); //the JWT principal is valid for 20 minutes

                JwtPayload payload = JwtPayload.newBuilder()
                        .withPublicClaims(accessKeyVO.getUser().getRole().name(),
                                String.valueOf(accessKeyVO.getUser().getId()),
                                accessKeyVO.getPermissions(),
                                accessKeyVO.getKey())
                        .withExpirationDate(expirationDate)
                        .buildAccessToken();

                return ResponseFactory.response(OK, tokenService.generateJwtAccessToken(payload));
            case REFRESH_TOKEN:
                logger.debug("JwtToken: requesting access by refresh token");

                break;
            case PASSWORD:
                //TODO: get user by login and if success generate JWT principal by particular claims
                break;
            default:
                return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_GRANT_TYPE));
        }

        return ResponseFactory.response(OK, null);
    }
}
