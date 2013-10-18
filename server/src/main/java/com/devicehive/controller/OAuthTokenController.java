package com.devicehive.controller;

import com.devicehive.configuration.Constants;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessToken;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.service.OAuthGrantService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.Response.Status.*;

@Path("/oauth2/token")
@Consumes(APPLICATION_FORM_URLENCODED)
@LogExecutionTime
public class OAuthTokenController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenController.class);
    @EJB
    private OAuthGrantService grantService;
    @EJB
    private TimestampService timestampService;

    @POST
    @PermitAll
    public Response accessTokenRequest(@FormParam("grant_type") @NotNull String grantType,
                                       @FormParam("code") String code,
                                       @FormParam("redirect_uri") String redirectUri,
                                       @FormParam("client_id") String clientId,
                                       @FormParam("scope") String scope,
                                       @FormParam("username") String login,
                                       @FormParam("password") String password) {
        logger.debug("OAuthToken: token requested. Grant type: {}, code: {}, redirect URI: {}, client id: {}",
                grantType, code, redirectUri, clientId);
        OAuthClient client = ThreadLocalVariablesKeeper.getOAuthClient();
        AccessKey key;
        switch (grantType) {
            case "authorization_code":
                if (clientId == null && client != null) {
                    clientId = client.getOauthId();
                }
                if (clientId == null) {
                    return ResponseFactory.response(BAD_REQUEST,
                            new ErrorResponse(BAD_REQUEST.getStatusCode(), "Client id is required!"));
                }
                key = grantService.accessTokenRequestForCodeType(code, redirectUri, clientId);
                break;
            case "password":
                if (client == null) {
                    return ResponseFactory.response(UNAUTHORIZED,
                            new ErrorResponse(UNAUTHORIZED.getStatusCode(), "Not authorized!"));
                }
                key = grantService.accessTokenRequestForPasswordType(scope, login, password, client);
                break;
            default:
                return ResponseFactory.response(BAD_REQUEST,
                        new ErrorResponse(BAD_REQUEST.getStatusCode(), "Invalid grant type!"));
        }
        AccessToken token = new AccessToken();
        token.setTokenType(Constants.KEY_AUTH);
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
