package com.devicehive.model.oauth;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.devicehive.configuration.Constants.UTF8;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * Created by tmatvienko on 1/9/15.
 */
@Component
public class GithubAuthProvider extends AuthProvider {
    private static final Logger logger = LoggerFactory.getLogger(GithubAuthProvider.class);
    
    private static final String GITHUB_PROVIDER_NAME = "Github";
    private IdentityProvider identityProvider;

    @Autowired
    private IdentityProviderService identityProviderService;
    @Autowired
    private Environment env;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private UserService userService;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private IdentityProviderUtils identityProviderUtils;

    @PostConstruct
    private void initialize() {
        identityProvider = identityProviderService.find(Long.parseLong(env.getProperty(Constants.GITHUB_IDENTITY_PROVIDER_ID)));
    }

    @Override
    public boolean isIdentityProviderAllowed() {
        return Boolean.valueOf(configurationService.get(Constants.GITHUB_IDENTITY_ALLOWED));
    }

    @Override
    public AccessKey createAccessKey(@NotNull final AccessKeyRequest request) {
        if (isIdentityProviderAllowed()) {
            if (request.getCode() != null) {
                final String accessToken = getAccessToken(request.getCode());
                final String email = getIdentityProviderEmail(accessToken);
                final User user = findUser(email);
                return accessKeyService.authenticate(user);
            }
            logger.error(Messages.INVALID_AUTH_CODE);
            throw new HiveException(Messages.INVALID_AUTH_CODE, Response.Status.BAD_REQUEST.getStatusCode());
        } else {
            logger.error(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, GITHUB_PROVIDER_NAME));
            throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_ALLOWED, GITHUB_PROVIDER_NAME),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    private String getAccessToken(final String code) {
        final String endpoint = identityProvider.getTokenEndpoint();
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", configurationService.get(Constants.GITHUB_IDENTITY_CLIENT_ID));
        params.put("client_secret", configurationService.get(Constants.GITHUB_IDENTITY_CLIENT_SECRET));
        final String response = identityProviderUtils.executePost(new NetHttpTransport(), params, endpoint, GITHUB_PROVIDER_NAME);
        List<NameValuePair> responseParams = URLEncodedUtils.parse(response, Charset.forName(UTF8));
        if (!"access_token".equals(responseParams.get(0).getName())) {
            logger.error("Exception has been caught during Identity Provider GET request execution", response);
            throw new HiveException(String.format(Messages.GETTING_OAUTH_ACCESS_TOKEN_FAILED, GITHUB_PROVIDER_NAME, response),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
        return responseParams.get(0).getValue();
    }

    private String getIdentityProviderEmail(final String accessToken) {
        final JsonElement jsonElement = identityProviderUtils.executeGet(new NetHttpTransport(),
                BearerToken.authorizationHeaderAccessMethod(), accessToken, identityProvider.getApiEndpoint(), GITHUB_PROVIDER_NAME);
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        Iterator<JsonElement> iterator = jsonArray.iterator();
        JsonObject jsonObject;
        while (iterator.hasNext()) {
            jsonObject = iterator.next().getAsJsonObject();
            if (jsonObject.get("primary").getAsBoolean()) {
                return jsonObject.get("email").getAsString();
            }
        }
        return null;
    }

    private User findUser(final String email) {
        final User user = userService.findGithubUser(email);
        if (user == null) {
            logger.error("No user with email {} found for identity provider {}", email, GITHUB_PROVIDER_NAME);
            throw new HiveException(Messages.USER_NOT_FOUND, Response.Status.UNAUTHORIZED.getStatusCode());
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            logger.error("User {} is locked, disabled or deleted", email);
            throw new HiveException(Messages.USER_NOT_ACTIVE, UNAUTHORIZED.getStatusCode());
        }
        return user;
    }
}
