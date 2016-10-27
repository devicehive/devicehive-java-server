package com.devicehive.model.oauth;

import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.vo.JwtTokenVO;

import javax.validation.constraints.NotNull;

/**
 * Provides JWT Token from OAuth2 authorization
 */
public abstract class AuthProvider {

    public abstract boolean isIdentityProviderAllowed();

    public abstract JwtTokenVO createAccessKey(@NotNull final AccessKeyRequestVO accessKeyRequest);
}
