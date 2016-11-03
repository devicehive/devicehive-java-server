package com.devicehive.model.oauth;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.IdentityProviderService;
import com.devicehive.service.OAuthTokenService;
import com.devicehive.service.UserService;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.vo.*;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.configuration.Constants.UTF8;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * Created by tmatvienko on 1/9/15.
 */
@Component
public class FacebookAuthProvider extends AuthProvider {
    private static final Logger logger = LoggerFactory.getLogger(FacebookAuthProvider.class);

    private static final String FACEBOOK_PROVIDER_NAME = "Facebook";

    private IdentityProviderVO identityProvider;

    @Autowired
    private IdentityProviderService identityProviderService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private UserService userService;
    @Autowired
    private OAuthTokenService tokenService;
    @Autowired
    private IdentityProviderUtils identityProviderUtils;

    @PostConstruct
    private void initialize() {
        identityProvider = identityProviderService.find(Constants.FACEBOOK_IDENTITY_PROVIDER_ID);
    }

    @Override
    public boolean isIdentityProviderAllowed() {
        return Boolean.valueOf(configurationService.get(Constants.FACEBOOK_IDENTITY_ALLOWED));
    }

    @Override
    public JwtTokenVO createAccessKey(@NotNull final AccessKeyRequestVO request) {
        if (isIdentityProviderAllowed()) {
            String accessToken;
            if (request.getCode() != null) {
                accessToken = getAccessToken(request.getCode(), request.getRedirectUri());
            } else {
                accessToken = request.getAccessToken();
                verifyAccessToken(accessToken);
            }
            final String userEmail = getIdentityProviderEmail(accessToken);
            final UserVO user = findUser(userEmail);
            return tokenService.authenticate(user);
        }
        logger.error(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, FACEBOOK_PROVIDER_NAME));
        throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, FACEBOOK_PROVIDER_NAME),
                Response.Status.UNAUTHORIZED.getStatusCode());
    }

    private String getAccessToken(final String code, final String redirectUrl) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(redirectUrl)) {
            logger.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
            throw new HiveException(Messages.INVALID_AUTH_REQUEST_PARAMETERS, Response.Status.BAD_REQUEST.getStatusCode());
        }
        final String endpoint = identityProvider.getTokenEndpoint();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", configurationService.get(Constants.FACEBOOK_IDENTITY_CLIENT_ID)));
        params.add(new BasicNameValuePair("client_secret", configurationService.get(Constants.FACEBOOK_IDENTITY_CLIENT_SECRET)));
        params.add(new BasicNameValuePair("redirect_uri", redirectUrl));
        final String response = identityProviderUtils.executeGet(new NetHttpTransport(), params, endpoint, FACEBOOK_PROVIDER_NAME);
        List<NameValuePair> responseParams = URLEncodedUtils.parse(response, Charset.forName(UTF8));
        if (!"access_token".equals(responseParams.get(0).getName())) {
            logger.error("Exception has been caught during Identity Provider GET request execution", response);
            throw new HiveException(String.format(Messages.GETTING_OAUTH_ACCESS_TOKEN_FAILED, FACEBOOK_PROVIDER_NAME, response),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
        return responseParams.get(0).getValue();
    }

    private void verifyAccessToken(final String accessToken) {
        final JsonElement verificationResponse =  identityProviderUtils.executeGet(new NetHttpTransport(),
                BearerToken.queryParameterAccessMethod(), accessToken,
                identityProvider.getVerificationEndpoint(), FACEBOOK_PROVIDER_NAME);
        final JsonElement verificationElement = verificationResponse.getAsJsonObject().get("id");
        final boolean isValid =  verificationElement != null && verificationElement.getAsString().equals(
                configurationService.get(Constants.FACEBOOK_IDENTITY_CLIENT_ID));
        if (!isValid) {
            logger.error("OAuth token verification for Facebook identity provider failed. Provider response: {}", verificationResponse);
            throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED,
                    FACEBOOK_PROVIDER_NAME), Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    private String getIdentityProviderEmail(final String accessToken) {
        final JsonElement jsonElement = identityProviderUtils.executeGet(new NetHttpTransport(),
                BearerToken.authorizationHeaderAccessMethod(), accessToken, identityProvider.getApiEndpoint(), FACEBOOK_PROVIDER_NAME);
        return jsonElement.getAsJsonObject().get("email").getAsString();
    }

    private UserVO findUser(final String email) {
        final UserVO user = userService.findFacebookUser(email);
        if (user == null) {
            logger.error("No user with email {} found for identity provider {}", email, FACEBOOK_PROVIDER_NAME);
            throw new HiveException(Messages.USER_NOT_FOUND, Response.Status.UNAUTHORIZED.getStatusCode());
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            logger.error("User {} is locked, disabled or deleted", email);
            throw new HiveException(Messages.USER_NOT_ACTIVE, UNAUTHORIZED.getStatusCode());
        }
        return user;
    }
}
