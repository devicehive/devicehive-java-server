package com.devicehive.client;


import com.devicehive.client.model.AccessType;
import com.devicehive.client.model.OAuthGrant;
import com.devicehive.client.model.OAuthType;

import java.sql.Timestamp;
import java.util.List;

/**
 * Client side controller for OAuth grants. Transport declared in the hive context will be used.
 */
public interface OAuthGrantController {

    /**
     * Queries OAuth grants
     *
     * @param userId        User identifier.
     * @param start         grant start timestamp (UTC).
     * @param end           grant end timestamp (UTC).
     * @param clientOauthId OAuth client OAuth identifier.
     * @param type          OAuth grant type.
     * @param scope         OAuth scope.
     * @param redirectUri   OAuth redirect URI.
     * @param accessType    access type.
     * @param sortField     Result list sort field. Available values are Timestamp (default).
     * @param sortOrder     Result list sort order. Available values are ASC and DESC
     * @param take          Number of records to take
     * @param skip          Number of records to skip
     * @return list of OAuth grants
     */
    List<OAuthGrant> list(long userId, Timestamp start, Timestamp end, String clientOauthId, OAuthType type,
                          String scope, String redirectUri, AccessType accessType, String sortField, String sortOrder,
                          Integer take, Integer skip);

    /**
     * Queries OAuth grants of the current user.
     *
     * @param start         grant start timestamp (UTC).
     * @param end           grant end timestamp (UTC).
     * @param clientOauthId OAuth client OAuth identifier.
     * @param type          OAuth grant type.
     * @param scope         OAuth scope.
     * @param redirectUri   OAuth redirect URI.
     * @param accessType    access type.
     * @param sortField     Result list sort field. Available values are Timestamp (default).
     * @param sortOrder     Result list sort order. Available values are ASC and DESC
     * @param take          Number of records to take
     * @param skip          Number of records to skip
     * @return list of OAuth grants
     */
    List<OAuthGrant> list(Timestamp start, Timestamp end, String clientOauthId, OAuthType type,
                          String scope, String redirectUri, AccessType accessType, String sortField, String sortOrder,
                          Integer take, Integer skip);

    /**
     * Gets information about OAuth grant.
     *
     * @param userId  user identifier
     * @param grantId grant identifier
     * @return OAuth grant associated with requested id.
     */
    OAuthGrant get(long userId, long grantId);

    /**
     * Gets information about OAuth grant of the current user.
     *
     * @param grantId grant identifier
     * @return OAuth grant associated with requested id.
     */
    OAuthGrant get(long grantId);

    /**
     * Creates new OAuth grant.
     *
     * @param userId user identifier
     * @param grant  grant to be created
     * @return created OAuth grant
     */
    OAuthGrant insert(long userId, OAuthGrant grant);

    /**
     * Creates new OAuth grant for current user.
     *
     * @param grant grant to be created
     * @return created OAuth grant
     */
    OAuthGrant insert(OAuthGrant grant);

    /**
     * Updates an existing OAuth grant.
     *
     * @param userId  User identifier
     * @param grantId Grant identifier
     * @param grant   grant resource providing update info
     * @return update OAuth grant
     */
    OAuthGrant update(long userId, long grantId, OAuthGrant grant);

    /**
     * Updates an existing OAuth grant of current user
     *
     * @param grantId Grant identifier
     * @param grant   grant resource providing update info
     * @return update OAuth grant
     */
    OAuthGrant update(long grantId, OAuthGrant grant);

    /**
     * Deletes an existing OAuth grant.
     *
     * @param userId  user identifier
     * @param grantId grant identifier
     */
    void delete(long userId, long grantId);

    /**
     * Deletes an existing OAuth grant of current user.
     *
     * @param grantId grant identifier
     */
    void delete(long grantId);
}
