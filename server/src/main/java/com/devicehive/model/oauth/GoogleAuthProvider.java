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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * Created by tmatvienko on 1/9/15.
 */
@Singleton
public class GoogleAuthProvider extends AuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAuthProvider.class);

    private static final String GOOGLE_PROVIDER_NAME = "Google";
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
        identityProvider = identityProviderService.find(Long.parseLong(propertiesService.getProperty(Constants.GOOGLE_IDENTITY_PROVIDER_ID)));
    }

    @Override
    public boolean isIdentityProviderAllowed() {
        return Boolean.valueOf(configurationService.get(Constants.GOOGLE_IDENTITY_ALLOWED));
    }

    @Override
    public AccessKey createAccessKey(@NotNull final AccessKeyRequest request) {
        if (isIdentityProviderAllowed()) {
            final String accessToken = request.getAccessToken() == null ? getAccessToken(request.getCode(), request.getRedirectUri()) : request.getAccessToken();
            final String userEmail = getIdentityProviderEmail(accessToken);
            final User user = findUser(userEmail);
            return accessKeyService.authenticate(user);
        }
        LOGGER.error(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, GOOGLE_PROVIDER_NAME));
        throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, GOOGLE_PROVIDER_NAME),
                Response.Status.UNAUTHORIZED.getStatusCode());
    }

    private String getAccessToken(final String code, final String redirectUrl) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(redirectUrl)) {
            LOGGER.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
            throw new HiveException(Messages.INVALID_AUTH_REQUEST_PARAMETERS, Response.Status.BAD_REQUEST.getStatusCode());
        }
        final String endpoint = identityProvider.getTokenEndpoint();
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", configurationService.get(Constants.GOOGLE_IDENTITY_CLIENT_ID));
        params.put("client_secret", configurationService.get(Constants.GOOGLE_IDENTITY_CLIENT_SECRET));
        params.put("redirect_uri", redirectUrl);
        params.put("grant_type", "authorization_code");
        final String response = identityProviderUtils.executePost(new NetHttpTransport(), params, endpoint, GOOGLE_PROVIDER_NAME);
        final JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
        try {
            return jsonObject.get("access_token").getAsString();
        } catch (IllegalStateException ex) {
            LOGGER.error("Exception has been caught during Identity Provider GET request execution", response);
            throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, GOOGLE_PROVIDER_NAME, response),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    private String getIdentityProviderEmail(final String accessToken) {
        final JsonElement verificationResponse = getVerificationResponse(accessToken);
        final JsonElement verificationElement = verificationResponse.getAsJsonObject().get("issued_to");
        final boolean isValid = verificationElement != null && verificationElement.getAsString().startsWith(
                configurationService.get(Constants.GOOGLE_IDENTITY_CLIENT_ID));
        if (!isValid) {
            LOGGER.error("OAuth token verification for Google identity provider failed. Provider response: {}", verificationResponse);
            throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED,
                    GOOGLE_PROVIDER_NAME), Response.Status.UNAUTHORIZED.getStatusCode());
        }
        return verificationResponse.getAsJsonObject().get("email").getAsString();
    }

    private User findUser(final String email) {
        final User user = userService.findGoogleUser(email);
        if (user == null) {
            LOGGER.error("No user with email {} found for identity provider {}", email, GOOGLE_PROVIDER_NAME);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, email),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            LOGGER.error(String.format(Messages.USER_NOT_ACTIVE, user.getId()));
            throw new HiveException(UNAUTHORIZED.getReasonPhrase(), UNAUTHORIZED.getStatusCode());
        }
        return user;
    }

    private JsonElement getVerificationResponse(final String accessToken) {
        return identityProviderUtils.executeGet(new NetHttpTransport(),
                BearerToken.queryParameterAccessMethod(), accessToken,
                identityProvider.getVerificationEndpoint(), GOOGLE_PROVIDER_NAME);
    }
}
