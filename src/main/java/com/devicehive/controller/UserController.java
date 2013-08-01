package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.UserDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.UserRequest;
import com.devicehive.model.response.UserResponse;
import com.devicehive.service.UserService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Path("/user")
public class UserController {

    @Inject
    private UserService userService;
    @Inject
    private UserDAO userDAO;
    @Context
    private ContainerRequestContext requestContext;

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getUsersList(@QueryParam("login") String login,
                                 @QueryParam("loginPattern") String loginPattern,
                                 @QueryParam("role") Integer role,
                                 @QueryParam("status") Integer status,
                                 @QueryParam("sortField") String sortField,
                                 @QueryParam("sortOrder") String sortOrder,
                                 @QueryParam("take") Integer take,
                                 @QueryParam("skip") Integer skip) {

        boolean sortOrderAsc = true;

        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"ID".equals(sortField) && !"Login".equals(sortField) && sortField != null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        //TODO validation for role and status
        List<User> result = userDAO.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.USERS_LISTED);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getUser(@PathParam("id") long id) {

        User user = userDAO.findUserWithNetworks(id);

        if (user == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("User not found."));
        }

        return ResponseFactory.response(Response.Status.OK,
                UserResponse.createFromUser(user),
                JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertUser(UserRequest user) {

        //neither we want left some params omitted
        if (user.getLogin() == null || user.getPassword() == null || user.getRole() == null
                || user.getStatus() == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }
        //nor we want these parameters to be null
        if (user.getLogin().getValue() == null || user.getPassword().getValue() == null
                || user.getRole().getValue() == null || user.getStatus().getValue() == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        if (userService.findByLogin(user.getLogin().getValue()) != null) {
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("User couldn't be created."));
        }

        User created = userService.createUser(user.getLogin().getValue(),
                                              user.getRoleEnum(),
                                              user.getStatusEnum(),
                                              user.getPassword().getValue());

        return ResponseFactory.response(Response.Status.CREATED, created, JsonPolicyDef.Policy.USERS_LISTED);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserRequest user, @PathParam("id") long userId) {

        if (user.getLogin() != null) {
            User u = userService.findByLogin(user.getLogin().getValue());

            if (u != null && u.getId() != userId) {
                return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("User couldn't be updated."));
            }
        }

        if (user.getLogin() != null && user.getLogin().getValue() == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        if (user.getPassword() != null && user.getPassword().getValue() == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        if (user.getRole() != null && user.getRole().getValue() == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        if (user.getStatus() != null && user.getStatus().getValue() == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        String loginValue = user.getLogin() == null ? null : user.getLogin().getValue();
        String passwordValue = user.getPassword() == null ? null : user.getPassword().getValue();

        userService.updateUser(userId, loginValue, user.getRoleEnum(), user.getStatusEnum(), passwordValue);

        return ResponseFactory.response(Response.Status.CREATED);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteUser(@PathParam("id") long userId) {

        userService.deleteUser(userId);

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    @GET
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {

        User existingUser = userDAO.findUserWithNetworks(id);
        if (existingUser == null) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("User not found."));
        }

        for (Network network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return ResponseFactory.response(Response.Status.OK, network, JsonPolicyDef.Policy.NETWORKS_LISTED);
            }
        }

        return ResponseFactory.response(Response.Status.NOT_FOUND);
    }

    @PUT
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response assignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {

        try {
            userService.assignNetwork(id, networkId);
        } catch (NotFoundException e) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("User or network not found"));
        }

        return ResponseFactory.response(Response.Status.CREATED);
    }

    @DELETE
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response unassignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {

        try {
            userService.unassignNetwork(id, networkId);
        } catch (NotFoundException e) {
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("User or network not found"));
        }

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    @GET
    @Path("/current")
    @PermitAll
    public Response getCurrent() {

        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("Couldn't get current user."));
        }

        User result = userService.findUserWithNetworksByLogin(login);

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    @PUT
    @Path("/current")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response updateCurrent(UserRequest ui) {

        String password = ui.getPassword().getValue();

        if (password == null) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("User couldn't be updated"));
        }

        User u = userService.findUserWithNetworksByLogin(login);

        userService.updatePassword(u.getId(), password);

        return ResponseFactory.response(Response.Status.CREATED);
    }


}
