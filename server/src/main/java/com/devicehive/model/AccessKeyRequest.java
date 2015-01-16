package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tmatvienko on 1/13/15.
 */
public class AccessKeyRequest implements HiveEntity {
    private static final long serialVersionUID = -3876398635939615946L;

    private String providerName;

    private String code;

    @SerializedName("redirect_url")
    private String redirectUri;

    @SerializedName("access_token")
    private String accessToken;

    private String login;

    private String password;

    public AccessKeyRequest() {
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
