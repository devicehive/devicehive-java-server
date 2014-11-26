package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.IdentityProviderDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.IdentityProvider;
import com.devicehive.util.HiveValidator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Created by tmatvienko on 11/17/14.
 */
@Stateless
public class IdentityProviderService {

    @EJB
    private IdentityProviderDAO identityProviderDAO;
    @EJB
    private HiveValidator hiveValidator;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IdentityProvider find(@NotNull Long id) {
        return identityProviderDAO.get(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IdentityProvider find(@NotNull String name) {
        return identityProviderDAO.get(name);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean delete(@NotNull Long id) {
        return identityProviderDAO.delete(id);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public IdentityProvider update(@NotNull Long identityProviderId, IdentityProvider identityProvider) {
        IdentityProvider existing = find(identityProviderId);
        if (existing == null) {
            throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, identityProviderId), NOT_FOUND.getStatusCode());
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
