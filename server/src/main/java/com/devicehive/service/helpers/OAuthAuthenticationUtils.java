package com.devicehive.service.helpers;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.enums.UserRole;
import com.devicehive.service.DeviceService;
import com.devicehive.service.IdentityProviderService;
import com.devicehive.service.NetworkService;
import com.devicehive.service.TimestampService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;

/**
 * Created by tmatvienko on 11/21/14.
 */
@Stateless
public class OAuthAuthenticationUtils {

    public static final String OAUTH_ACCESS_KEY_LABEL_FORMAT = "OAuth token for: %s";

    @EJB
    private NetworkService networkService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private PropertiesService propertiesService;
    @EJB
    private IdentityProviderService identityProviderService;
    @EJB
    private TimestampService timestampService;

    private Long googleIdentityProviderId;
    private Long facebookIdentityProviderId;
    private Long githubIdentityProviderId;

    @PostConstruct
    public void loadProperties() {
        googleIdentityProviderId = Long.valueOf(propertiesService.getProperty(Constants.GOOGLE_IDENTITY_PROVIDER_ID));
        facebookIdentityProviderId = Long.valueOf(propertiesService.getProperty(Constants.FACEBOOK_IDENTITY_PROVIDER_ID));
        githubIdentityProviderId = Long.valueOf(propertiesService.getProperty(Constants.GITHUB_IDENTITY_PROVIDER_ID));
    }

    public AccessKey prepareAccessKey(final User user, final String email) {
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(user);
        accessKey.setLabel(String.format(OAUTH_ACCESS_KEY_LABEL_FORMAT, email));
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        accessKey.setKey(keyProcessor.generateKey());
        Timestamp expirationDate = new Timestamp(timestampService.getTimestamp().getTime() +
                Long.parseLong(propertiesService.getProperty(Constants.DEFAULT_EXPIRATION_DATE_PROPERTY)));
        accessKey.setExpirationDate(expirationDate);
        return accessKey;
    }

    public AccessKeyPermission preparePermission(final UserRole userRole) {
        AccessKeyPermission permission = new AccessKeyPermission();
        switch (userRole) {
            case ADMIN:
                break;
            case CLIENT: default:
                permission.setActions(AvailableActions.getClientActions());
                break;
        }
        return permission;
    }

    public IdentityProvider getIdentityProvider(final String state) {
        if (!state.startsWith("identity_provider_id")) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS,
                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        final int index = state.indexOf("=") + 1;
        final String identityProviderId = state.substring(index, state.length());
        if (!StringUtils.isNumeric(identityProviderId)) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS,
                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        final Long providerId = Long.valueOf(identityProviderId);
        final IdentityProvider identityProvider = identityProviderService.find(providerId);
        if (identityProvider == null) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS,
                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        return identityProvider;
    }

    public boolean validateVerificationResponse(final JsonObject jsonObject, final IdentityProvider identityProvider) {
        JsonElement verificationElement;
        final Long providerId = identityProvider.getId();
        if (providerId.equals(googleIdentityProviderId)) {
            verificationElement = jsonObject.get("issued_to");
            return verificationElement != null && verificationElement.getAsString().startsWith(
                    configurationService.get(Constants.GOOGLE_IDENTITY_CLIENT_ID));
        } else if (providerId.equals(facebookIdentityProviderId)) {
            verificationElement = jsonObject.get("id");
            return verificationElement != null && verificationElement.getAsString().equals(
                    configurationService.get(Constants.FACEBOOK_IDENTITY_CLIENT_ID));
        }
        throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, identityProvider.getId()),
                Response.Status.BAD_REQUEST.getStatusCode());
    }

    public String getLoginFromResponse(final JsonObject jsonObject, @NotNull final Long providerId) throws HiveException {
        try {
            if (providerId.equals(googleIdentityProviderId)) {
                return jsonObject.getAsJsonArray("emails").get(0).getAsJsonObject().get("value").getAsString();
            } else if (providerId.equals(facebookIdentityProviderId)) {
                return jsonObject.get("email").getAsString();
            } else if (providerId.equals(githubIdentityProviderId)) {
                return jsonObject.get("login").getAsString();
            } else {
                throw new HiveException(Messages.IDENTITY_PROVIDER_NOT_FOUND, Response.Status.BAD_REQUEST.getStatusCode());
            }
        } catch (NullPointerException e) {
            throw new HiveException(Messages.WRONG_IDENTITY_PROVIDER_SCOPE, Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    public void validateActions(AccessKey accessKey) {
        for (AccessKeyPermission permission : accessKey.getPermissions()) {
            if (permission.getActionsAsSet() != null && !permission.getActionsAsSet().isEmpty()) {
                if (!AvailableActions.validate(permission.getActionsAsSet())) {
                    throw new HiveException(Messages.UNKNOWN_ACTION, Response.Status.BAD_REQUEST.getStatusCode());
                }
            }
        }
    }
}
