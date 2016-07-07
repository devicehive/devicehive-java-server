package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.IdentityProviderDao;
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
    private HiveValidator hiveValidator;
    @Autowired
    IdentityProviderDao identityProviderDao;

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider find(@NotNull Long id) {
        return identityProviderDao.find(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider find(@NotNull String name) {
        return identityProviderDao.getByName(name);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean delete(@NotNull Long id) {
        return identityProviderDao.deleteById(id);
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
        return identityProviderDao.merge(existing);
    }
}
