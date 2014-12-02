package com.devicehive.controller;


import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessToken;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.OAuthGrantService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.auth.AllowedKeyAction.Action.MANAGE_ACCESS_KEY;
import static com.devicehive.configuration.Constants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.Response.Status.*;

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

    @EJB
    private AccessKeyService accessKeyService;

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

    @Path("/apply")
    @POST
    @RolesAllowed({HiveRoles.KEY})
    @AllowedKeyAction(action = MANAGE_ACCESS_KEY)
    public Response login() {
        return ResponseFactory.response(OK, hiveSecurityContext.getHivePrincipal().getKey(),
                JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED);
    }

    @Path("/remove")
    @DELETE
    @RolesAllowed({HiveRoles.KEY})
    @AllowedKeyAction(action = MANAGE_ACCESS_KEY)
    public Response logout() {
        AccessKey accessKey = hiveSecurityContext.getHivePrincipal().getKey();
        accessKeyService.delete(null, accessKey.getId());
        return ResponseFactory.response(OK);
    }
}
