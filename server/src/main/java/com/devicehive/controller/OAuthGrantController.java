package com.devicehive.controller;


import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.*;
import com.devicehive.model.updates.OAuthGrantUpdate;
import com.devicehive.service.OAuthGrantService;
import com.devicehive.service.UserService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/user/{userId}/oauth/grant")
@LogExecutionTime
public class OAuthGrantController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthGrantController.class);
    private OAuthGrantService grantService;
    private UserService userService;

    @EJB
    public void setGrantService(OAuthGrantService grantService) {
        this.grantService = grantService;
    }

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GET
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response list(@PathParam("userId") String userId,
                         @QueryParam("start") Timestamp start,
                         @QueryParam("end") Timestamp end,
                         @QueryParam("clientOAuthId") String clientOAuthId,
                         @QueryParam("type") String type,
                         @QueryParam("scope") String scope,
                         @QueryParam("redirectUri") String redirectUri,
                         @QueryParam("accessType") String accessType,
                         @QueryParam("sortField") @DefaultValue("Timestamp") String sortField,
                         @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                         @QueryParam("take") Integer take,
                         @QueryParam("skip") Integer skip) {
        logger.debug("OAuthGrant: list requested. User id: {}, start: {}, end: {}, clientOAuthID: {}, type: {}, " +
                "scope: {}, redirectURI: {}, accessType: {}, sortField: {}, sortOrder: {}, take: {}, skip: {}",
                userId, start, end, clientOAuthId, type, scope, redirectUri, accessType, sortField, sortOrder, take,
                skip);
        if (!sortField.equalsIgnoreCase("Timestamp")) {
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        } else {
            sortField = sortField.toLowerCase();
        }
        User user = getUser(userId);
        List<OAuthGrant> result = grantService.list(user, start, end, clientOAuthId,
                type == null ? null : Type.forName(type).ordinal(), scope,
                redirectUri, accessType == null ? null : AccessType.forName(accessType).ordinal(), sortField,
                sortOrder, take, skip);
        logger.debug(
                "OAuthGrant: list proceed successfully. User id: {}, start: {}, end: {}, clientOAuthID: {}, " +
                        "type: {}, scope: {}, redirectURI: {}, accessType: {}, sortField: {}, sortOrder: {}, take: {}, skip: {}",
                userId, start, end, clientOAuthId, type, scope, redirectUri, accessType, sortField, sortOrder, take,
                skip);
        if (user.isAdmin()) {
            return ResponseFactory.response(OK, result, OAUTH_GRANT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, result, OAUTH_GRANT_LISTED);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response get(@PathParam("userId") String userId,
                        @PathParam("id") long grantId) {
        logger.debug("OAuthGrant: get requested. User id: {}, grant id: {}", userId, grantId);
        User user = getUser(userId);
        OAuthGrant grant = grantService.get(user, grantId);
        if (grant == null) {
            throw new HiveException("Grant with such id not found!", NOT_FOUND.getStatusCode());
        }
        logger.debug("OAuthGrant: proceed successfully. User id: {}, grant id: {}", userId, grantId);
        if (user.isAdmin()) {
            return ResponseFactory.response(OK, grant, OAUTH_GRANT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, grant, OAUTH_GRANT_LISTED);
    }

    @POST
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response insert(@PathParam("userId") String userId,
                           @JsonPolicyApply(OAUTH_GRANT_PUBLISHED) OAuthGrant grant) {
        logger.debug("OAuthGrant: insert requested. User id: {}, grant: {}", userId, grant);
        User user = getUser(userId);
        grantService.save(grant, user);
        logger.debug("OAuthGrant: insert proceed successfully. User id: {}, grant: {}", userId, grant);
        if (grant.getType().equals(Type.TOKEN)) {
            return ResponseFactory.response(CREATED, grant, OAUTH_GRANT_SUBMITTED_TOKEN);
        } else {
            return ResponseFactory.response(CREATED, grant, OAUTH_GRANT_SUBMITTED_CODE);
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response update(@PathParam("userId") String userId,
                           @PathParam("id") Long grantId,
                           @JsonPolicyApply(OAUTH_GRANT_PUBLISHED) OAuthGrantUpdate grant) {
        logger.debug("OAuthGrant: update requested. User id: {}, grant id: {}", userId, grantId);
        User user = getUser(userId);
        OAuthGrant updated = grantService.update(user, grantId, grant);
        if (updated == null) {
            throw new HiveException("Grant with such id not found!", NOT_FOUND.getStatusCode());
        }
        logger.debug("OAuthGrant: update proceed successfully. User id: {}, grant id: {}", userId, grantId);
        if (updated.getType().equals(Type.TOKEN)) {
            return ResponseFactory.response(OK, updated, OAUTH_GRANT_SUBMITTED_TOKEN);
        } else {
            return ResponseFactory.response(OK, updated, OAUTH_GRANT_SUBMITTED_CODE);
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response delete(@PathParam("userId") String userId,
                           @PathParam("id") Long grantId) {
        logger.debug("OAuthGrant: delete requested. User id: {}, grant id: {}", userId, grantId);
        User user = getUser(userId);
        grantService.delete(user, grantId);
        logger.debug("OAuthGrant: delete proceed successfully. User id: {}, grant id: {}", userId, grantId);
        return ResponseFactory.response(NO_CONTENT);
    }

    private User getUser(String userId) {
        User current = ThreadLocalVariablesKeeper.getPrincipal().getUser();
        if (userId.equalsIgnoreCase(Constants.CURRENT_USER)) {
            return current;
        }
        if (StringUtils.isNumeric(userId)) {
            Long id = Long.parseLong(userId);
            if (current.getId().equals(id)) {
                return current;
            } else if (current.isAdmin()) {
                User result = userService.findById(id);
                if (result == null) {
                    throw new HiveException("User with such id not found!", NOT_FOUND.getStatusCode());
                }
                return result;
            }
            throw new HiveException("Not authorized!", UNAUTHORIZED.getStatusCode());
        }
        throw new HiveException("Bad user identifier: " + userId, BAD_REQUEST.getStatusCode());
    }

}
