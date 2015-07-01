package com.devicehive.resource;

import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

public interface OAuthTokenResource {
    String AUTHORIZATION_CODE = "authorization_code";
    String PASSWORD = "password";

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PreAuthorize("permitAll")
    Response accessTokenRequest(@FormParam(GRANT_TYPE) @NotNull String grantType,
                                @FormParam(CODE) String code,
                                @FormParam(REDIRECT_URI) String redirectUri,
                                @FormParam(CLIENT_ID) String clientId,
                                @FormParam(SCOPE) String scope,
                                @FormParam(USERNAME) String login,
                                @FormParam(PASSWORD) String password);
}
