package com.devicehive.resource;

import com.devicehive.model.OAuthClient;
import com.devicehive.model.updates.OAuthClientUpdate;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.*;

@Path("/oauth/client")
public interface OAuthClientResource {

    @GET
    @PreAuthorize("permitAll")
    Response list(
            @QueryParam(NAME) String name,
            @QueryParam(NAME_PATTERN) String namePattern,
            @QueryParam(DOMAIN) String domain,
            @QueryParam(OAUTH_ID) String oauthId,
            @QueryParam(SORT_FIELD) String sortField,
            @QueryParam(SORT_ORDER) String sortOrderSt,
            @QueryParam(TAKE) Integer take,
            @QueryParam(SKIP) Integer skip
    );

    @GET
    @Path("/{id}")
    @PreAuthorize("permitAll")
    Response get(
            @PathParam(ID) long clientId);

    @POST
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    Response insert(
            OAuthClient clientToInsert);

    @PUT
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    Response update(
            @PathParam(ID) Long clientId,
            OAuthClientUpdate clientToUpdate);

    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    Response delete(
            @PathParam(ID) Long clientId);
}
