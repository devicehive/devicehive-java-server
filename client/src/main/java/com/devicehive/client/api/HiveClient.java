package com.devicehive.client.api;


import com.devicehive.client.model.ApiInfo;

public interface HiveClient {

    ApiInfo getInfo();

    void authenticate(String login, String pasword);
}
