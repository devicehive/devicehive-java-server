package com.devicehive.resource;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.updates.OAuthClientUpdate;
import com.devicehive.service.OAuthClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

@Service
@Path("/oauth/client")
public class OAuthClientResource {
    private static final Logger logger = LoggerFactory.getLogger(OAuthClientResource.class);

    @Autowired
    private OAuthClientService clientService;

    @GET
    @PreAuthorize("permitAll")
    public Response list(@QueryParam(NAME) String name,
                         @QueryParam(NAME_PATTERN) String namePattern,
                         @QueryParam(DOMAIN) String domain,
                         @QueryParam(OAUTH_ID) String oauthId,
                         @QueryParam(SORT_FIELD) String sortField,
                         @QueryParam(SORT_ORDER) String sortOrderSt,
                         @QueryParam(TAKE) Integer take,
                         @QueryParam(SKIP) Integer skip) {
        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !sortField.equalsIgnoreCase(ID) && !sortField.equalsIgnoreCase(NAME) &&
            !sortField.equalsIgnoreCase(DOMAIN) && !sortField.equalsIgnoreCase(OAUTH_ID)) {
            return ResponseFactory.response(BAD_REQUEST,
                                            new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                              Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }

        List<OAuthClient> result =
            clientService.get(name, namePattern, domain, oauthId, sortField, sortOrder, take, skip);
        logger.debug("OAuthClient list procced. Params: name {}, namePattern {}, domain {}, oauthId {}, " +
                     "sortField {}, sortOrder {}, take {}, skip {}. Result list contains {} elems", name, namePattern,
                     domain, oauthId, sortField, sortOrder, take, skip, result.size());

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal != null && principal.getUser() != null && principal.getUser().isAdmin()) {
            return ResponseFactory.response(OK, result, OAUTH_CLIENT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, result, OAUTH_CLIENT_LISTED);
    }

    @GET
    @Path("/{id}")
    @PreAuthorize("permitAll")
    public Response get(@PathParam(ID) long clientId) {
        logger.debug("OAuthClient get requested. Client id: {}", clientId);
        OAuthClient existing = clientService.get(clientId);
        if (existing == null) {
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              "OAuthClient with id " + clientId + " not found"));
        }
        logger.debug("OAuthClient proceed successfully. Client id: {}", clientId);
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal != null && principal.getUser() != null && principal.getUser().isAdmin()) {
            return ResponseFactory.response(OK, existing, OAUTH_CLIENT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, existing, OAUTH_CLIENT_LISTED);
    }

    @POST
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    public Response insert(OAuthClient clientToInsert) {
        logger.debug("OAuthClient insert requested. Client to insert: {}", clientToInsert);
        if (clientToInsert == null) {
            return ResponseFactory.response(BAD_REQUEST,
                                            new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                                              Messages.INVALID_REQUEST_PARAMETERS));
        }
        OAuthClient created = clientService.insert(clientToInsert);
        logger.debug("OAuthClient insert procceed successfully. Client to insert: {}. New id: {}", clientToInsert,
                     clientToInsert.getId());
        return ResponseFactory.response(CREATED, created, OAUTH_CLIENT_PUBLISHED);
    }

    @PUT
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    public Response update(@PathParam(ID) Long clientId, OAuthClientUpdate clientToUpdate) {
        logger.debug("OAuthClient update requested. Client id: {}", clientId);
        clientService.update(clientToUpdate, clientId);
        logger.debug("OAuthClient update proceed successfully. Client id: {}", clientId);
        return ResponseFactory.response(NO_CONTENT);
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    public Response delete(@PathParam(ID) Long clientId) {
        logger.debug("OAuthClient delete requested");
        clientService.delete(clientId);
        logger.debug("OAuthClient with id = {} is deleted", clientId);
        return ResponseFactory.response(NO_CONTENT);
    }
}