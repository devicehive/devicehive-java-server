package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.UserDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
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
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response getUsersList(
            @QueryParam("login") String login,
            @QueryParam("loginPattern") String loginPattern,
            @QueryParam("role") Integer role,
            @QueryParam("status") Integer status,
            @QueryParam("sortField") String sortField,
            @QueryParam("sortOrder") String sortOrder,
            @QueryParam("take") Integer take,
            @QueryParam("skip") Integer skip
    ) {
        boolean sortOrderAsc = true;

        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }

        if (!"ID".equals(sortField) && !"Login".equals(sortField) && sortField != null) {  //ID??
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        //TODO validation for role and status
        List<User> result = userDAO.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);

        return Response.ok().entity(result).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(HiveRoles.ADMIN)
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public Response getUser(@PathParam("id") long id) {
        User user = userDAO.findUserWithNetworks(id);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok().entity(UserResponse.createFromUser(user)).build();
    }

    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response insertUser(UserRequest user) {
        //neither we want left some params omitted
        if (user.getLogin() == null || user.getPassword() == null || user.getRole() == null
                || user.getStatus() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        //nor we want these parameters to be null
        if (user.getLogin().getValue() == null || user.getPassword().getValue() == null
                || user.getRole().getValue() == null || user.getStatus().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (userService.findByLogin(user.getLogin().getValue()) != null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User created = userService.createUser(user.getLogin().getValue(), user.getRoleEnum(), user.getStatusEnum(), user.getPassword().getValue());

        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserRequest user, @PathParam("id") long userId) {

        if (user.getLogin() != null) {
            User u = userService.findByLogin(user.getLogin().getValue());

            if (u != null && u.getId() != userId) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }

        if (user.getLogin() != null && user.getLogin().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (user.getPassword() != null && user.getPassword().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (user.getRole() != null && user.getRole().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (user.getStatus() != null && user.getStatus().getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String loginValue = user.getLogin() == null ? null : user.getLogin().getValue();
        String passwordValue = user.getPassword() == null ? null : user.getPassword().getValue();

        userService.updateUser(userId, loginValue, user.getRoleEnum(), user.getStatusEnum(), passwordValue);

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteUser(@PathParam("id") long userId) {
        if (!userService.deleteUser(userId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {

        User existingUser = userDAO.findUserWithNetworks(id);
        if (existingUser == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        for (Network network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                Annotation[] annotations =
                        {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NETWORKS_LISTED)};
                Response.ok().entity(network, annotations).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.assignNetwork(id, networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unassignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.unassignNetwork(id, networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/current")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public Response getCurrent() {
        String login = requestContext.getSecurityContext().getUserPrincipal().getName();
        if (login == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(userService.findUserWithNetworksByLogin(login)).build();
    }

    @PUT
    @Path("/current")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public Response updateCurrent(UserRequest ui) {

        String password = ui.getPassword().getValue();

        if (password == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User u = userService.findUserWithNetworksByLogin(login);

        userService.updatePassword(u.getId(), password);
        return Response.status(Response.Status.CREATED).build();
    }


}
