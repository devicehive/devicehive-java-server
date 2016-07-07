package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.rdbms.GenericDaoImpl;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.IdentityProvider;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Created by tmatvienko on 11/17/14.
 */
@Component
public class IdentityProviderService {

    @Autowired
    private GenericDaoImpl genericDAO;
    @Autowired
    private HiveValidator hiveValidator;

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider find(@NotNull Long id) {
        return genericDAO.find(IdentityProvider.class, id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider find(@NotNull String name) {
        return genericDAO.createNamedQuery(IdentityProvider.class, "IdentityProvider.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean delete(@NotNull Long id) {
        int result = genericDAO.createNamedQuery("IdentityProvider.deleteById", Optional.of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
        return result > 0;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public IdentityProvider update(@NotNull Long identityProviderId, IdentityProvider identityProvider) {
        IdentityProvider existing = find(identityProviderId);
        if (existing == null) {
            throw new IllegalParametersException(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, identityProviderId));
        }
        if (identityProvider.getName() != null) {
            existing.setName(identityProvider.getName());
        }
        if (identityProvider.getApiEndpoint() != null) {
            existing.setApiEndpoint(identityProvider.getApiEndpoint());
        }
        hiveValidator.validate(existing);
        return genericDAO.merge(existing);
    }
}
