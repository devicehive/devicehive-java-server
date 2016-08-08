package com.devicehive.model.oauth;

import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.vo.AccessKeyVO;

import javax.validation.constraints.NotNull;

/**
 * Created by tmatvienko on 1/9/15.
 */
public abstract class AuthProvider {

    public abstract boolean isIdentityProviderAllowed();

    public abstract AccessKeyVO createAccessKey(@NotNull final AccessKeyRequestVO accessKeyRequest);
}
