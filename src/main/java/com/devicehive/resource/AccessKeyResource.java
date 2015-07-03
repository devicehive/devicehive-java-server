package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.AccessKey;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.wordnik.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;

/**
 * REST Resource for access keys: <i>/user/{userId}/accesskey</i> See <a href="http://www.devicehive.com/restful/#Reference/AccessKey">DeviceHive
 * RESTful API: AccessKey</a> for details.
 */
@Path("/user/{userId}/accesskey")
@Api(tags = {"user-access-key"}, description = "Access key operations")
public interface AccessKeyResource {

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/list">DeviceHive RESTful API:
     * AccessKey: list</a> Gets list of access keys and their permissions.
     *
     * @param userId User identifier. Use the 'current' keyword to list access keys of the current user.
     * @return If successful, this method returns array of <a href="http://www.devicehive.com/restful#Reference/AccessKey/">AccessKey</a> resources in the response body according to the
     * specification.
     */
    @GET
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    @ApiOperation(value = "List access keys", notes = "Returns a list of access keys based on provided parameters")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request parameters invalid")
    })
    Response list(
            @ApiParam(name = "userId", value = "User Id", required = true)
            @PathParam("userId")
            String userId,
            @ApiParam(name = "label", value = "Access Key label")
            @QueryParam("label")
            String label,
            @ApiParam(name = "labelPattern", value = "Access Key label pattern (e.g. %value%)")
            @QueryParam("labelPattern")
            String labelPattern,
            @ApiParam(name = "type", value = "Access Key type")
            @QueryParam("type")
            Integer type,
            @ApiParam(name = "sortField", value = "Access Key field to sort by")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Sort order", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Limit", defaultValue = "20")
            @QueryParam("take")
            Integer take,
            @ApiParam(name = "skip", value = "Offset", defaultValue = "0")
            @QueryParam("skip")
            Integer skip);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/get">DeviceHive RESTful API:
     * AccessKey: get</a> Gets information about access key and its permissions.
     *
     * @param userId      User identifier. Use the 'current' keyword to get access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive .com/restful#Reference/AccessKey/">AccessKey</a>
     * resource in the response body according to the specification.
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    @ApiOperation(value = "Get user's access key", notes = "Return a key by user id and access key id")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If access key is not found")
    })
    Response get(
            @ApiParam(name = "userId", value = "User Id")
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Access Key Id")
            @PathParam("id")
            long accessKeyId);

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/insert">DeviceHive RESTful API:
     * AccessKey: insert</a> Creates new access key.
     *
     * @param userId User identifier. Use the 'current' keyword to get access key of the current user.
     * @return If successful, this method returns an <a href="http://www.devicehive .com/restful#Reference/AccessKey/">AccessKey</a>
     * resource in the response body according to the specification.
     */
    @POST
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    @ApiOperation(value = "Create Access key", notes = "Create access key for provided user")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response insert(
            @ApiParam(name = "userId", value = "User Id")
            @PathParam("userId")
            String userId,
            @ApiParam(value = "Access Key Body", defaultValue =
                    "{\n" +
                            "   \"type\": 0,\n" +
                            "   \"label\": \"Access key label\",\n" +
                            "}", required = true)
            @JsonPolicyApply(ACCESS_KEY_PUBLISHED)
            AccessKey key);

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
    @ApiOperation(value = "Update Access key", notes = "Updates an existing access key")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If access key is not found")
    })
    Response update(
            @ApiParam(name = "userId", value = "User Id")
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Access Key Id")
            @PathParam("id")
            Long accessKeyId,
            @JsonPolicyApply(ACCESS_KEY_PUBLISHED)
            @ApiParam(value = "Access Key Body", required = true, defaultValue = "{\n" +
                    "   \"type\": 0,\n" +
                    "   \"label\": \"Access key label\",\n" +
                    "}")
            AccessKeyUpdate accessKeyUpdate);

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
    @ApiOperation(value = "Delete Access key", notes = "Delete an existing access key")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response delete(
            @ApiParam(name = "userId", value = "User Id")
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Access Key Id")
            @PathParam("id")
            Long accessKeyId);
}
