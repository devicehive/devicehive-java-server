package com.devicehive.resource.impl;


import com.devicehive.configuration.Messages;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessToken;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.resource.OAuthTokenResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.OAuthGrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.OAUTH_AUTH_SCEME;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class OAuthTokenResourceImpl implements OAuthTokenResource {
    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenResourceImpl.class);

    @Autowired
    private OAuthGrantService grantService;

    @Override
    public Response accessTokenRequest(String grantType, String code, String redirectUri, String clientId, String scope, String login, String password) {
        logger.debug("OAuthToken: token requested. Grant type: {}, code: {}, redirect URI: {}, client id: {}",
                     grantType, code, redirectUri, clientId);
        OAuthClient client = (OAuthClient) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AccessKey key;
        switch (grantType) {
            case AUTHORIZATION_CODE:
                if (clientId == null && client != null) {
                    clientId = client.getOauthId();
                }
                if (clientId == null) {
                    return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.CLIENT_ID_IS_REQUIRED));
                }
                key = grantService.accessTokenRequestForCodeType(code, redirectUri, clientId);
                break;
            case PASSWORD:
                if (client == null) {
                    return ResponseFactory.response(UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED.getStatusCode(), Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                key = grantService.accessTokenRequestForPasswordType(scope, login, password, client);
                break;
            default:
                return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), Messages.INVALID_GRANT_TYPE));
        }
        AccessToken token = new AccessToken();
        token.setTokenType(OAUTH_AUTH_SCEME);
        token.setAccessToken(key.getKey());
        Long expiresIn = key.getExpirationDate() == null
                         ? null
                         : key.getExpirationDate().getTime() * 1000; //time in seconds
        token.setExpiresIn(expiresIn);
        logger.debug("OAuthToken: token requested. Grant type: {}, code: {}, redirect URI: {}, client id: {}",
                     grantType, code, redirectUri, clientId);
        return ResponseFactory.response(OK, token);
    }
}
