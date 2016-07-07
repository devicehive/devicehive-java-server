package com.devicehive.dao;

import com.devicehive.model.IdentityProvider;

import javax.validation.constraints.NotNull;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface IdentityProviderDao {

    IdentityProvider getByName(@NotNull String name);

    boolean deleteById(@NotNull Long id);

    IdentityProvider find(@NotNull Long id);

    IdentityProvider merge(IdentityProvider existing);
}
