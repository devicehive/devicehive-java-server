package com.devicehive.resource.impl;

import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPrincipal;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.AccessKeyVO;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

@Service
public class JwtTokenResourceImpl implements JwtTokenResource {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenResourceImpl.class);

    @Autowired
    private JwtClientService tokenService;

    @Override
    public Response tokenRequest(
            @ApiParam(name = "access_key", value = "Access key", required = true)
            @FormParam("access_key")
            @NotNull
            String accessKey,
            @ApiParam(name = "grant_type", value = "Grant type", required = true)
            @FormParam("grant_type")
            @NotNull
            String grantType,
            @ApiParam(name = "client_credentials", value = "Client credentials", required = true)
            @FormParam("client_credentials")
            String clientCredentials) {


        switch (grantType) {
            case AUTH_HEADER:
                //TODO: go to DB and check by key if user exists
                logger.debug("JwtToken: get access key VO by key {}", accessKey);
                AccessKeyVO accessKeyVO = tokenService.getAccessKey(accessKey);
                if (accessKeyVO == null) {
                    return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                logger.debug("JwtToken: proceed successfully. AccessKey : {}", accessKey);
                JwtPrincipal jwtPrincipal = new JwtPrincipal();
                jwtPrincipal.setAccessToken(accessKeyVO.getKey());
                jwtPrincipal.setClientId(String.valueOf(accessKeyVO.getUser().getId()));
                jwtPrincipal.setExpiresIn(accessKeyVO.getExpirationDate().getTime());
                jwtPrincipal.setRole(accessKeyVO.getUser().getRole().name());
                jwtPrincipal.setTokenType(accessKeyVO.getType().name());
                jwtPrincipal.setPermissions(accessKeyVO.getPermissions());
                return ResponseFactory.response(OK, jwtPrincipal);
            case REFRESH_TOKEN:
                //TODO: refresh token logic
                break;
            case PASSWORD:
                //TODO: get token by password
                break;
            default:
                return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_GRANT_TYPE));
        }

        return ResponseFactory.response(OK, null);
    }
}
