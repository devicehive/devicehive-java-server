package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.IdentityProviderDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.IdentityProvider;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Created by tmatvienko on 11/17/14.
 */
@Component
public class IdentityProviderService {

    @Autowired
    private IdentityProviderDAO identityProviderDAO;
    @Autowired
    private HiveValidator hiveValidator;

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider find(@NotNull Long id) {
        return identityProviderDAO.get(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider find(@NotNull String name) {
        return identityProviderDAO.get(name);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean delete(@NotNull Long id) {
        return identityProviderDAO.delete(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IdentityProvider update(@NotNull Long identityProviderId, IdentityProvider identityProvider) {
        IdentityProvider existing = find(identityProviderId);
        if (existing == null) {
            throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, identityProviderId), BAD_REQUEST.getStatusCode());
        }
        if (identityProvider.getName() != null) {
            existing.setName(identityProvider.getName());
        }
        if (identityProvider.getApiEndpoint() != null) {
            existing.setApiEndpoint(identityProvider.getApiEndpoint());
        }
        hiveValidator.validate(existing);
        return identityProviderDAO.update(existing);
    }
}
