package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.AccessKey;
import com.devicehive.model.updates.AccessKeyUpdate;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;

/**
 * REST Resource for access keys: <i>/user/{userId}/accesskey</i> See <a href="http://www.devicehive.com/restful/#Reference/AccessKey">DeviceHive
 * RESTful API: AccessKey</a> for details.
 */
public interface AccessKeyResource {

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/list">DeviceHive RESTful API:
     * AccessKey: list</a> Gets list of access keys and their permissions.
     *
     * @param userId User identifier. Use the 'current' keyword to list access keys of the current user.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/AccessKey/">AccessKey</a> resources in the response body according to the
     *         specification.
     */
    @GET
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    Response list(
            @PathParam(USER_ID) String userId,
            @QueryParam(LABEL) String label,
            @QueryParam(LABEL_PATTERN) String labelPattern,
            @QueryParam(TYPE) Integer type,
            @QueryParam(SORT_FIELD) String sortField,
            @QueryParam(SORT_ORDER) String sortOrderSt,
            @QueryParam(TAKE) Integer take,
            @QueryParam(SKIP) Integer skip);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/get">DeviceHive RESTful API:
     * AccessKey: get</a> Gets information about access key and its permissions.
     *
     * @param userId      User identifier. Use the 'current' keyword to get access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive .com/restful#Reference/AccessKey/">AccessKey</a>
     *         resource in the response body according to the specification.
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    Response get(
            @PathParam(USER_ID) String userId,
            @PathParam(ID) long accessKeyId);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/insert">DeviceHive RESTful API:
     * AccessKey: insert</a> Creates new access key.
     *
     * @param userId User identifier. Use the 'current' keyword to get access key of the current user.
     * @return If successful, this method returns an <a href="http://www.devicehive .com/restful#Reference/AccessKey/">AccessKey</a>
     *         resource in the response body according to the specification.
     */
    @POST
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    Response insert(
            @PathParam(USER_ID) String userId,
            @JsonPolicyApply(ACCESS_KEY_PUBLISHED) AccessKey key);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/update">DeviceHive RESTful API:
     * AccessKey: update</a> Updates an existing access key.
     *
     * @param userId      User identifier. Use the 'current' keyword to update access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive .com/restful#Reference/AccessKey/">AccessKey</a>
     * resource in the response body according to the specification.
     */
    @PUT
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    Response update(
            @PathParam(USER_ID) String userId,
            @PathParam(ID) Long accessKeyId,
            @JsonPolicyApply(ACCESS_KEY_PUBLISHED) AccessKeyUpdate accessKeyUpdate);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/delete">DeviceHive RESTful API:
     * AccessKey: delete</a>
     *
     * @param userId      User identifier. Use the 'current' keyword to update access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive .com/restful#Reference/AccessKey/">AccessKey</a>
     * resource in the response body according to the specification.
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    Response delete(
            @PathParam(USER_ID) String userId,
            @PathParam(ID) Long accessKeyId);
}
