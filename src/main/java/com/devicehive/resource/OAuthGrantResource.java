package com.devicehive.resource;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.updates.OAuthGrantUpdate;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_PUBLISHED;

public interface OAuthGrantResource {

    @GET
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    Response list(
            @PathParam(USER_ID) String userId,
            @QueryParam(START) String startTs,
            @QueryParam(END) String endTs,
            @QueryParam(CLIENT_OAUTH_ID) String clientOAuthId,
            @QueryParam(TYPE) String type,
            @QueryParam(SCOPE) String scope,
            @QueryParam(REDIRECT_URI) String redirectUri,
            @QueryParam(ACCESS_TYPE) String accessType,
            @QueryParam(SORT_FIELD) @DefaultValue(TIMESTAMP) String sortField,
            @QueryParam(SORT_ORDER) String sortOrderSt,
            @QueryParam(TAKE) Integer take,
            @QueryParam(SKIP) Integer skip
    );

    @GET
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    Response get(
            @PathParam(USER_ID) String userId,
            @PathParam(ID) long grantId
    );

    @POST
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    Response insert(
            @PathParam(USER_ID) String userId,
            @JsonPolicyApply(OAUTH_GRANT_PUBLISHED) OAuthGrant grant
    );

    @PUT
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    Response update(
            @PathParam(USER_ID) String userId,
            @PathParam(ID) Long grantId,
            @JsonPolicyApply(OAUTH_GRANT_PUBLISHED) OAuthGrantUpdate grant
    );

    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_GRANT')")
    Response delete(
            @PathParam(USER_ID) String userId,
            @PathParam(ID) Long grantId
    );
}
