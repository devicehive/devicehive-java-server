package com.devicehive.service.helpers;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.User;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.oauth.IdentityProviderEnum;
import com.devicehive.service.DeviceService;
import com.devicehive.service.IdentityProviderService;
import com.devicehive.service.NetworkService;
import com.devicehive.service.time.TimestampService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.Date;

/**
 * Created by tmatvienko on 11/21/14.
 */
@Component
public class OAuthAuthenticationUtils {

    @Autowired
    private NetworkService networkService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private IdentityProviderService identityProviderService;
    @Autowired
    private TimestampService timestampService;

    public AccessKey prepareAccessKey(final User user) {
        AccessKey accessKey = new AccessKey();
        accessKey.setUser(user);
        accessKey.setLabel(String.format(Messages.OAUTH_TOKEN_LABEL, user.getLogin(), System.currentTimeMillis()));
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        accessKey.setKey(keyProcessor.generateKey());
        Date expirationDate = new Date(timestampService.getTimestamp().getTime() +
                configurationService.getLong(Constants.SESSION_TIMEOUT, Constants.DEFAULT_SESSION_TIMEOUT));
        accessKey.setExpirationDate(expirationDate);
        accessKey.setType(AccessKeyType.SESSION);
        return accessKey;
    }

    public AccessKeyPermission preparePermission(final UserRole userRole) {
        AccessKeyPermission permission = new AccessKeyPermission();
        switch (userRole) {
            case ADMIN:
                break;
            case CLIENT: default:
                permission.setActionsArray(AvailableActions.getClientActions());
                break;
        }
        return permission;
    }

    public IdentityProviderEnum getIdentityProvider(final String providerName) {
        return IdentityProviderEnum.forName(providerName);
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
