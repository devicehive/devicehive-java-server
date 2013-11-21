package com.devicehive.client.api.client;


import com.devicehive.client.model.OAuthClient;

import java.util.List;

/**
 * Client side controller for OAuth clients. Transport declared in the hive context will be used.
 */
public interface OAuthClientController {

    /**
     * Queries OAuth clients.
     *
     * @param name        client name.
     * @param namePattern client name pattern.
     * @param domain      domain.
     * @param oauthId     OAuth client ID.
     * @param sortField   Result list sort field. Available values are ID, Name, Domain and OAuthID.
     * @param sortOrder   Result list sort order. Available values are ASC and DESC.
     * @param take        Number of records to take.
     * @param skip        Number of records to skip.
     * @return list of OAuth clients
     */
    List<OAuthClient> list(String name, String namePattern, String domain, String oauthId, String sortField,
                           String sortOrder, Integer take, Integer skip);

    /**
     * Gets information about OAuth client.
     *
     * @param id OAuth client identifier.
     * @return OAuth client associated with requested id.
     */
    OAuthClient get(long id);

    /**
     * Creates new OAuth client.
     *
     * @param client client to be inserted
     * @return OAuthClient resource with client identifier and client OAuth secret.
     */
    OAuthClient insert(OAuthClient client);

    /**
     * Updates an existing OAuth client.
     *
     * @param id     OAuth client identifier.
     * @param client OAuth client resource update info.
     */
    void update(long id, OAuthClient client);

    /**
     * Deletes an existing OAuth client.
     *
     * @param id OAuth client identifier.
     */
    void delete(long id);
}
