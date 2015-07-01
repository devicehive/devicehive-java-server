package com.devicehive.model.oauth;

import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyRequest;

import javax.validation.constraints.NotNull;

/**
 * Created by tmatvienko on 1/9/15.
 */
public abstract class AuthProvider {

    public abstract boolean isIdentityProviderAllowed();

    public abstract AccessKey createAccessKey(@NotNull final AccessKeyRequest accessKeyRequest);
}
