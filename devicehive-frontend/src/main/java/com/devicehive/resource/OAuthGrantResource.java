package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.updates.OAuthGrantUpdate;
import com.devicehive.vo.OAuthGrantVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_PUBLISHED;

@Api(tags = {"OAuthGrant"})
@Path("/user/{userId}/oauth/grant")
public interface OAuthGrantResource {

    @GET
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    @ApiOperation(value = "List oAuth grants", notes = "Returns list of oAuth grants for user")
    Response list(
            @ApiParam(name = "userId", value = "User Id", required = true)
            @PathParam("userId")
            String userId,
            @ApiParam(name = "start", value = "Start timestamp")
            @QueryParam("start")
            String startTs,
            @ApiParam(name = "end", value = "End timestamp")
            @QueryParam("end")
            String endTs,
            @ApiParam(name = "clientOAuthId", value = "Client oAuth id")
            @QueryParam("clientOAuthId")
            String clientOAuthId,
            @ApiParam(name = "type", value = "Type")
            @QueryParam("type")
            String type,
            @ApiParam(name = "scope", value = "Scope")
            @QueryParam("scope")
            String scope,
            @ApiParam(name = "redirectUri", value = "Redirect uri")
            @QueryParam("redirectUri")
            String redirectUri,
            @ApiParam(name = "accessType", value = "Access type")
            @QueryParam("accessType")
            String accessType,
            @ApiParam(name = "sortField", value = "Sort field")
            @QueryParam("sortField")
            @DefaultValue("timestamp")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Sort order")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Limit param")
            @QueryParam("take")
            Integer take,
            @ApiParam(name = "skip", value = "Skip param")
            @QueryParam("skip")
            Integer skip);

    @GET
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    @ApiOperation(value = "Get oAuth grant", notes = "Returns oAuth grant by user and id")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If grant not found")
    })
    Response get(
            @ApiParam(name = "userId", value = "User Id", required = true)
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Grant Id", required = true)
            @PathParam("id")
            long grantId);

    @POST
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    @ApiOperation(value = "Create oAuth grant")
    Response insert(
            @ApiParam(name = "userId", value = "User Id", required = true)
            @PathParam("userId")
            String userId,
            @JsonPolicyApply(OAUTH_GRANT_PUBLISHED)
            @ApiParam(value = "Grant body", defaultValue = "{}", required = true)
            OAuthGrantVO grant
    );

    @PUT
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    @ApiOperation(value = "Update oAuth grant")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If grant not found")
    })
    Response update(
            @ApiParam(name = "userId", value = "User Id", required = true)
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Grant Id", required = true)
            @PathParam("id")
            Long grantId,
            @ApiParam(value = "Grant body", defaultValue = "{}", required = true)
            @JsonPolicyApply(OAUTH_GRANT_PUBLISHED)
            OAuthGrantUpdate grant
    );

    @DELETE
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    @ApiOperation(value = "Delete oAuth grant")
    Response delete(
            @ApiParam(name = "userId", value = "User Id", required = true)
            @PathParam("userId")
            String userId,
            @ApiParam(name = "id", value = "Grant Id", required = true)
            @PathParam("id")
            Long grantId);
}
