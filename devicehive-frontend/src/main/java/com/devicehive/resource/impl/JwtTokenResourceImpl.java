package com.devicehive.resource.impl;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.JwtTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;
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

    @Autowired
    private UserService userService;

    @Override
    public Response tokenRequest(String token, String grantType, String clientCredentials, String username, String password) {
        JwtPayload jwtPayload = tokenService.getPayload(token);

        long currentTimeMillis = System.currentTimeMillis();
        //the JWT payload is valid for 20 minutes
        Date expirationDate = new Date(currentTimeMillis + Constants.DEFAULT_JWT_ACCESS_TOKEN_MAX_AGE);

        //fixme
        /*switch (grantType) {
            case AUTH_HEADER:
                logger.debug("JwtToken: check token type {}", token);
                if (jwtPayload.getType() == TokenType.ACCESS) {
                    String accessKey = jwtPayload.getToken();
                    logger.debug("JwtToken: validate token by access key {}", accessKey);
                    AccessKeyVO accessKeyVO = accessKeyService.getAccessKey(accessKey);
                    if (accessKeyVO == null) {
                        return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                    }
                    logger.debug("JwtToken: proceed successfully. AccessKey : {}", accessKeyVO);

                    jwtPayload = JwtPayload.newBuilder()
                            .withPublicClaims(accessKeyVO.getUser().getRole().name(),
                                    String.valueOf(accessKeyVO.getUser().getId()),
                                    accessKeyVO.getPermissions(),
                                    accessKeyVO.getKey())
                            .withExpirationDate(expirationDate)
                            .buildAccessToken();
                }
                break;
            case REFRESH_TOKEN:
                logger.debug("JwtToken: requesting access by refresh token");
                if (jwtPayload.getType() != TokenType.REFRESH) {
                    return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_REQUEST_PARAMETERS));
                }

                String accessKey = jwtPayload.getToken();
                AccessKeyVO accessKeyVO = accessKeyService.getAccessKey(accessKey);

                Long userId = Long.parseLong(jwtPayload.getClientId());
                UserVO refreshUser = userService.findById(userId);

                if (refreshUser == null || accessKeyVO == null) {
                    return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                logger.debug("JwtToken: access by refresh token processed successfully. User : {}", refreshUser);

                jwtPayload = JwtPayload.newBuilder()
                        .withPublicClaims(accessKeyVO.getUser().getRole().name(),
                                String.valueOf(accessKeyVO.getUser().getId()),
                                accessKeyVO.getPermissions(),
                                accessKeyVO.getKey())
                        .withExpirationDate(expirationDate)
                        .buildAccessToken();
                break;
            case PASSWORD:
                logger.debug("JwtToken: requesting access by user's login / password");
                //authenticate user using credentials
                UserVO passwordUser = userService.authenticate(username, password);
                if (passwordUser == null) {
                    return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                logger.debug("JwtToken: access by user's login / password proceed successfully. User : {}", passwordUser);

                //create JWT token by user's info
                jwtPayload = JwtPayload.newBuilder()
                        .withPublicClaims(passwordUser.getRole().name(),
                                String.valueOf(passwordUser.getId()),
                                null,
                                null)
                        .withExpirationDate(expirationDate)
                        .buildAccessToken();
                break;
            default:
                return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_GRANT_TYPE));
        }*/

        return ResponseFactory.response(OK, tokenService.generateJwtAccessToken(jwtPayload));
    }
}
