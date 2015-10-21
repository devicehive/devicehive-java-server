package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.AccessKey;
import com.devicehive.model.updates.AccessKeyUpdate;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;

/**
 * REST Resource for access keys: <i>/user/{userId}/accesskey</i> See <a href="http://www.devicehive.com/restful/#Reference/AccessKey">DeviceHive
 * RESTful API: AccessKey</a> for details.
 */
@Path("/user/{userId}/accesskey")
@Api(tags = {"AccessKey"}, description = "Represents an access key to this API.", consumes="application/json")
@Produces({"application/json"})
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
    @ApiOperation(value = "List access keys", notes = "Gets list of access keys and their permissions.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns array of AccessKey resources in the response body.", response = AccessKey.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If user with given userId is not found")
    })
    Response list(
            @ApiParam(name = "userId", value = "User identifier. Use the 'current' keyword to list access keys of the current user.", required = true)
            @PathParam("userId")
            String userId,
            @ApiParam(name = "label", value = "Filter by access key label.")
            @QueryParam("label")
            String label,
            @ApiParam(name = "labelPattern", value = "Filter by access key label pattern.")
            @QueryParam("labelPattern")
            String labelPattern,
            @ApiParam(name = "type", value = "Filter by access key type.")
            @QueryParam("type")
            Integer type,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "ID,Label")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Result list sort order.", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Number of records to take from the result list.", defaultValue = "20")
            @QueryParam("take")
            Integer take,
            @ApiParam(name = "skip", value = "Number of records to skip from the result list.", defaultValue = "0")
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
    @ApiOperation(value = "Get user's access key", notes = "Gets information about access key and its permissions.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a AccessKey resource in the response body.", response = AccessKey.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If access key is not found")
    })
    Response get(
            @ApiParam(name = "userId", value = "User identifier. Use the 'current' keyword to get access key of the current user.")
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Access key identifier.")
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
    @ApiOperation(value = "Create Access key", notes = "Creates new access key.")
    @ApiResponses({
            @ApiResponse(code = 201, message = "If successful, this method returns a AccessKey resource in the response body.", response = AccessKey.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response insert(
            @ApiParam(name = "userId", value = "User identifier. Use the 'current' keyword to create access key for the current user.")
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
    @ApiOperation(value = "Update Access key", notes = "Updates an existing access key.")
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response update(
            @ApiParam(name = "userId", value = "User identifier. Use the 'current' keyword to update access key of the current user.")
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Access key identifier.")
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
    @ApiOperation(value = "Delete Access key", notes = "Deletes an existing access key.")
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If access key is not found")
    })
    Response delete(
            @ApiParam(name = "userId", value = "User identifier. Use the 'current' keyword to delete access key of the current user.")
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Access key identifier.")
            @PathParam("id")
            Long accessKeyId);
}
