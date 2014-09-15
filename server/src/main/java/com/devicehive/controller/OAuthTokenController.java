package com.devicehive.controller;


import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessToken;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.service.OAuthGrantService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.CLIENT_ID;
import static com.devicehive.configuration.Constants.CODE;
import static com.devicehive.configuration.Constants.GRANT_TYPE;
import static com.devicehive.configuration.Constants.OAUTH_AUTH_SCEME;
import static com.devicehive.configuration.Constants.REDIRECT_URI;
import static com.devicehive.configuration.Constants.SCOPE;
import static com.devicehive.configuration.Constants.USERNAME;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/oauth2/token")
@Consumes(APPLICATION_FORM_URLENCODED)
@LogExecutionTime
public class OAuthTokenController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenController.class);
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String PASSWORD = "password";

    @EJB
    private OAuthGrantService grantService;

    @EJB
    private TimestampService timestampService;

    @Inject
    private HiveSecurityContext hiveSecurityContext;

    @POST
    @PermitAll
    public Response accessTokenRequest(@FormParam(GRANT_TYPE) @NotNull String grantType,
                                       @FormParam(CODE) String code,
                                       @FormParam(REDIRECT_URI) String redirectUri,
                                       @FormParam(CLIENT_ID) String clientId,
                                       @FormParam(SCOPE) String scope,
                                       @FormParam(USERNAME) String login,
                                       @FormParam(PASSWORD) String password) {
        logger.debug("OAuthToken: token requested. Grant type: {}, code: {}, redirect URI: {}, client id: {}",
                     grantType, code, redirectUri, clientId);
        OAuthClient client = hiveSecurityContext.getoAuthClient();
        AccessKey key;
        switch (grantType) {
            case AUTHORIZATION_CODE:
                if (clientId == null && client != null) {
                    clientId = client.getOauthId();
                }
                if (clientId == null) {
                    return ResponseFactory.response(BAD_REQUEST,
                                                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                                      Messages.CLIENT_ID_IS_REQUIRED));
                }
                key = grantService.accessTokenRequestForCodeType(code, redirectUri, clientId);
                break;
            case PASSWORD:
                if (client == null) {
                    return ResponseFactory.response(UNAUTHORIZED,
                                                    new ErrorResponse(UNAUTHORIZED.getStatusCode(),
                                                                      Messages.UNAUTHORIZED_REASON_PHRASE));
                }
                key = grantService.accessTokenRequestForPasswordType(scope, login, password, client);
                break;
            default:
                return ResponseFactory.response(BAD_REQUEST,
                                                new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                                  Messages.INVALID_GRANT_TYPE));
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
