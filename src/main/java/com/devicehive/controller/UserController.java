package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.UserInsert;
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

    @Context
    private ContainerRequestContext requestContext;

    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USERS_LISTED)
    public List<User> getUsersList(
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
            throw new BadRequestException("The sort order cannot be equal " + sortOrder);
        }
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"login".equals(sortField) && !"id".equals(sortField) && sortField != null) {  //ID??
            throw new HiveException("The sort field cannot be equal " + sortField);//maybe better to do sort field null
        }
        //TODO validation for role and status

        return userService.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);

    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(HiveRoles.ADMIN)
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public User getUser(@PathParam("id") long id) {
        User u = userService.findUserWithNetworks(id);

        if (u == null) {
            throw new NotFoundException("No such user");
        }

        return u;
    }

    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertUser(UserInsert user) {
        //neither we want left some params omitted
        if (user.getLogin() == null || user.getPassword() == null || user.getRole() == null || user.getStatus() == null) {
            throw new BadRequestException();
        }
        //nor we want these parameters to be null
        if (user.getLogin().getValue() == null || user.getPassword().getValue() == null
                || user.getRole().getValue() == null || user.getStatus().getValue() == null) {
            throw new BadRequestException();
        }

        User u = userService.findByLogin(user.getLogin().getValue());

        if (u != null) {
            throw new ForbiddenException("User with such login already exists");
        }
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.USERS_LISTED)};
        return Response.status(Response.Status.CREATED).entity(u, annotations).build();
    }


    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserInsert user, @PathParam("id") long userId) {

        if (user.getLogin() != null) {
            User u = userService.findByLogin(user.getLogin().getValue());

            if (u != null && u.getId() != userId) {
                throw new ForbiddenException("User with such login already exists");
            }
        }
        if (user.getLogin() != null && user.getLogin().getValue() == null) {
            throw new BadRequestException();
        }

        if (user.getPassword() != null && user.getPassword().getValue() == null) {
            throw new BadRequestException();
        }

        if (user.getRole() != null && user.getRole().getValue() == null) {
            throw new BadRequestException();
        }

        if (user.getStatus() != null && user.getStatus().getValue() == null) {
            throw new BadRequestException();
        }
        String loginValue = user.getLogin() == null ? null : user.getLogin().getValue();
        String passwordValue = user.getPassword() == null ? null : user.getPassword().getValue();

        userService.updateUser(userId, loginValue, user.getRoleEnum(), user.getStatusEnum(), passwordValue);

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response updateUser(@PathParam("id") long userId) {
        userService.deleteUser(userId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }


    @GET
    @Path("/{id}/network/{networkId}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Network getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            User u = userService.findUserWithNetworks(id);
            for (Network n : u.getNetworks()) {
                if (n.getId() == networkId) {
                    return n;
                }
            }
        } catch (Exception e) {
            throw new NotFoundException();
        }
        throw new NotFoundException();
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
    public User getCurrent() {
        String login = requestContext.getSecurityContext().getUserPrincipal().getName();
        if (login == null) {
            throw new ForbiddenException("User Must be authenticated");
        }
        return userService.findUserWithNetworksByLogin(login);
    }

    @PUT
    @Path("/current")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateCurrent(UserInsert ui) {

        String password = ui.getPassword().getValue();

        if (password == null) {
            throw new BadRequestException();
        }

        String login = requestContext.getSecurityContext().getUserPrincipal().getName();

        if (login == null) {
            throw new ForbiddenException("User Must be authenticated");
        }

        User u = userService.findUserWithNetworksByLogin(login);

        userService.updatePassword(u.getId(), password);
        return Response.status(Response.Status.CREATED).build();
    }


}
