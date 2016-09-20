package com.devicehive.resource.impl;

import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPrincipal;
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

    private static final int JWT_AGE_IN_MILLISECONDS = 600000 * 2; // 20 minutes

    @Autowired
    private JwtClientService tokenService;

    @Autowired
    private AccessKeyService accessKeyService;

    @Override
    public Response tokenRequest(String accessKey, String grantType, String clientCredentials) {


        switch (grantType) {
            case AUTH_HEADER:
                //TODO: go to DB and check by key if user exists
                logger.debug("JwtToken: get access key VO by key {}", accessKey);
                AccessKeyVO accessKeyVO = accessKeyService.getAccessKey(accessKey);
                if (accessKeyVO == null) {
                    return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                logger.debug("JwtToken: proceed successfully. AccessKey : {}", accessKey);

                long currentTimeMillis = System.currentTimeMillis();
                Date now = new Date(currentTimeMillis);
                Date expiredDate = new Date(currentTimeMillis + JWT_AGE_IN_MILLISECONDS); //the JWT principal is valid for 20 minutes

                JwtPrincipal jwtPrincipal = new JwtPrincipal(
                        String.valueOf(accessKeyVO.getUser().getId()),
                        accessKeyVO.getUser().getRole().name(),
                        accessKeyVO.getKey(),
                        accessKeyVO.getType().name(),
                        expiredDate,
                        now);
                jwtPrincipal.setPermissions(accessKeyVO.getPermissions());

                return ResponseFactory.response(OK, tokenService.generateJwtAccessToken(jwtPrincipal));
            case REFRESH_TOKEN:
                //TODO: refresh token logic - need regenerate token here
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
