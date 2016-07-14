package com.devicehive.dao;

import com.devicehive.model.IdentityProvider;

import javax.validation.constraints.NotNull;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface IdentityProviderDao {

    IdentityProvider getByName(@NotNull String name);

    /**
     * Delete identity provider by name.
     * @param name identity provider name
     */
    boolean deleteById(@NotNull String name);

    IdentityProvider merge(IdentityProvider existing);

    void persist(IdentityProvider identityProvider);
}
