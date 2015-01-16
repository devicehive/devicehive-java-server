package com.devicehive.model.oauth;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyRequest;
import com.devicehive.model.IdentityProvider;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.IdentityProviderService;
import com.devicehive.service.UserService;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
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
@Singleton
public class FacebookAuthProvider extends AuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAuthProvider.class);

    private static final String FACEBOOK_PROVIDER_NAME = "Facebook";
    private IdentityProvider identityProvider;

    @EJB
    private IdentityProviderService identityProviderService;
    @EJB
    private PropertiesService propertiesService;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private UserService userService;
    @EJB
    private AccessKeyService accessKeyService;
    @EJB
    private IdentityProviderUtils identityProviderUtils;

    @PostConstruct
    private void initialize() {
        identityProvider = identityProviderService.find(Long.parseLong(propertiesService.getProperty(Constants.FACEBOOK_IDENTITY_PROVIDER_ID)));
    }

    @Override
    public boolean isIdentityProviderAllowed() {
        return Boolean.valueOf(configurationService.get(Constants.FACEBOOK_IDENTITY_ALLOWED));
    }

    @Override
    public AccessKey createAccessKey(@NotNull final AccessKeyRequest request) {
        if (isIdentityProviderAllowed()) {
            String accessToken;
            if (request.getCode() != null) {
                accessToken = getAccessToken(request.getCode(), request.getRedirectUri());
            } else {
                accessToken = request.getAccessToken();
                verifyAccessToken(accessToken);
            }
            final String userEmail = getIdentityProviderEmail(accessToken);
            final User user = findUser(userEmail);
            return accessKeyService.authenticate(user);
        }
        LOGGER.error(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, FACEBOOK_PROVIDER_NAME));
        throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, FACEBOOK_PROVIDER_NAME),
                Response.Status.UNAUTHORIZED.getStatusCode());
    }

    private String getAccessToken(final String code, final String redirectUrl) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(redirectUrl)) {
            LOGGER.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
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
            LOGGER.error("Exception has been caught during Identity Provider GET request execution", response);
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
            LOGGER.error("OAuth token verification for Facebook identity provider failed. Provider response: {}", verificationResponse);
            throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED,
                    FACEBOOK_PROVIDER_NAME), Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    private String getIdentityProviderEmail(final String accessToken) {
        final JsonElement jsonElement = identityProviderUtils.executeGet(new NetHttpTransport(),
                BearerToken.authorizationHeaderAccessMethod(), accessToken, identityProvider.getApiEndpoint(), FACEBOOK_PROVIDER_NAME);
        return jsonElement.getAsJsonObject().get("email").getAsString();
    }

    private User findUser(final String email) {
        final User user = userService.findFacebookUser(email);
        if (user == null) {
            LOGGER.error("No user with email {} found for identity provider {}", email, FACEBOOK_PROVIDER_NAME);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, email),
                    Response.Status.NOT_FOUND.getStatusCode());
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            LOGGER.error(String.format(Messages.USER_NOT_ACTIVE, user.getId()));
            throw new HiveException(UNAUTHORIZED.getReasonPhrase(), UNAUTHORIZED.getStatusCode());
        }
        return user;
    }
}
