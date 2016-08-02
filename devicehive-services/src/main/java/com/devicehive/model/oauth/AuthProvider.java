package com.devicehive.model.oauth;

import com.devicehive.model.AccessKey;
import com.devicehive.vo.AccessKeyRequestVO;

import javax.validation.constraints.NotNull;

/**
 * Created by tmatvienko on 1/9/15.
 */
public abstract class AuthProvider {

    public abstract boolean isIdentityProviderAllowed();

    public abstract AccessKey createAccessKey(@NotNull final AccessKeyRequestVO accessKeyRequest);
}
