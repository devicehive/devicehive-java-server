package com.devicehive.dao;

import com.devicehive.vo.IdentityProviderVO;

import javax.validation.constraints.NotNull;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface IdentityProviderDao {

    IdentityProviderVO getByName(@NotNull String name);

    /**
     * Delete identity provider by name.
     * @param name identity provider name
     */
    boolean deleteById(@NotNull String name);

    IdentityProviderVO merge(IdentityProviderVO existing);

    void persist(IdentityProviderVO identityProvider);
}
