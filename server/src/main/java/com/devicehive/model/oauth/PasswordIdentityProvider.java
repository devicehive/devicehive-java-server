package com.devicehive.model.oauth;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyRequest;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

/**
 * Created by tmatvienko on 1/13/15.
 */
@Singleton
public class PasswordIdentityProvider extends AuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordIdentityProvider.class);

    private static final String PASSWORD_PROVIDER_NAME = "Password";

    @EJB
    private UserService userService;
    @EJB
    private AccessKeyService accessKeyService;

    @Override
    public boolean isIdentityProviderAllowed() {
        return true;
    }

    @Override
    public AccessKey createAccessKey(@NotNull final AccessKeyRequest request) {
        if (StringUtils.isBlank(request.getLogin()) || StringUtils.isBlank(request.getPassword())) {
            LOGGER.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
            throw new HiveException(Messages.INVALID_AUTH_REQUEST_PARAMETERS, Response.Status.BAD_REQUEST.getStatusCode());
        }
        final User user = findUser(request.getLogin(), request.getPassword());
        return accessKeyService.authenticate(user);
    }

    private User findUser(String login, String password) {
        return userService.findUser(login, password);
    }
}
