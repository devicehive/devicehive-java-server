package com.devicehive.client;


import com.devicehive.client.model.OAuthClient;
import com.devicehive.client.model.exceptions.HiveException;

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
                           String sortOrder, Integer take, Integer skip) throws HiveException;

    /**
     * Gets information about OAuth client.
     *
     * @param id OAuth client identifier.
     * @return OAuth client associated with requested id.
     */
    OAuthClient get(long id) throws HiveException;

    /**
     * Creates new OAuth client.
     *
     * @param client client to be inserted
     * @return OAuthClient resource with client identifier and client OAuth secret.
     */
    OAuthClient insert(OAuthClient client) throws HiveException;

    /**
     * Updates an existing OAuth client.
     *
     * @param client OAuth client resource update info.
     */
    void update(OAuthClient client) throws HiveException;

    /**
     * Deletes an existing OAuth client.
     *
     * @param id OAuth client identifier.
     */
    void delete(long id) throws HiveException;
}
