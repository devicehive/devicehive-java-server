package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.updates.OAuthClientUpdate;
import com.devicehive.service.OAuthClientService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/oauth/client")
@LogExecutionTime
public class OAuthClientController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthClientController.class);
    private OAuthClientService clientService;

    @EJB
    public void setClientDAO(OAuthClientService clientService) {
        this.clientService = clientService;
    }

    @GET
    @PermitAll
    public Response list(@QueryParam("name") String name,
                         @QueryParam("namePattern") String namePattern,
                         @QueryParam("domain") String domain,
                         @QueryParam("oauthId") String oauthId,
                         @QueryParam("sortField") String sortField,
                         @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                         @QueryParam("take") Integer take,
                         @QueryParam("skip") Integer skip) {
        logger.debug("OAuthClient list requested. Params: name {}, namePattern {}, domain {}, oauthId {}, " +
                "sortField {}, sortOrder {}, take {}, skip {}", name, namePattern, domain, oauthId, sortField,
                sortOrder, take, skip);

        if (sortField != null && !sortField.equalsIgnoreCase("ID") && !sortField.equalsIgnoreCase("Name") &&
                !sortField.equalsIgnoreCase("Domain") && !sortField.equalsIgnoreCase("OAuthID")) {
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
            if (sortField.equalsIgnoreCase("OAuthID")) {
                sortField = "oauthId";
            }
        }

        List<OAuthClient> result =
                clientService.get(name, namePattern, domain, oauthId, sortField, sortOrder, take, skip);
        logger.debug("OAuthClient list procced. Params: name {}, namePattern {}, domain {}, oauthId {}, " +
                "sortField {}, sortOrder {}, take {}, skip {}. Result list contains {} elems", name, namePattern,
                domain, oauthId, sortField, sortOrder, take, skip, result.size());

        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (principal != null && principal.getUser() != null && principal.getUser().isAdmin()) {
            return ResponseFactory.response(OK, result, OAUTH_CLIENT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, result, OAUTH_CLIENT_LISTED);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response get(@PathParam("id") long clientId) {
        logger.debug("OAuthClient get requested. Client id: {}", clientId);
        OAuthClient existing = clientService.get(clientId);
        if (existing == null) {
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), "OAuthClient with id " + clientId + " not found"));
        }
        logger.debug("OAuthClient proceed successfully. Client id: {}", clientId);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (principal != null && principal.getUser() != null && principal.getUser().isAdmin()) {
            return ResponseFactory.response(OK, existing, OAUTH_CLIENT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, existing, OAUTH_CLIENT_LISTED);
    }

    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    public Response insert(OAuthClient clientToInsert) {
        logger.debug("OAuthClient insert requested. Client to insert: {}", clientToInsert);
        if (clientToInsert == null) {
            return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        OAuthClient created = clientService.insert(clientToInsert);
        logger.debug("OAuthClient insert procceed successfully. Client to insert: {}. New id: {}", clientToInsert,
                clientToInsert.getId());
        return ResponseFactory.response(CREATED, created, OAUTH_CLIENT_PUBLISHED);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response update(@PathParam("id") Long clientId, OAuthClientUpdate clientToUpdate) {
        logger.debug("OAuthClient update requested. Client id: {}", clientId);
        clientService.update(clientToUpdate, clientId);
        logger.debug("OAuthClient update proceed successfully. Client id: {}", clientId);
        return ResponseFactory.response(NO_CONTENT);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response delete(@PathParam("id") Long clientId) {
        logger.debug("OAuthClient delete requested");
        clientService.delete(clientId);
        logger.debug("OAuthClient with id = {} is deleted", clientId);
        return ResponseFactory.response(NO_CONTENT);
    }
}
