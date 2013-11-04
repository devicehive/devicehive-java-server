package com.devicehive.client.api;


import com.devicehive.client.model.OAuthClient;

import java.util.List;

public interface OAuthClientController {

    List<OAuthClient> list(String name, String namePattern, String domain, String oauthId, String sortField,
                           String sortOrder, Integer take, Integer skip);

    OAuthClient get(long id);

    OAuthClient insert(OAuthClient client);

    void update(long id, OAuthClient client);

    void delete(long id);
}
