package com.devicehive.controller;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.UserInsert;
import com.devicehive.model.response.DetailedUserResponse;
import com.devicehive.model.response.SimpleNetworkResponse;
import com.devicehive.model.response.SimpleUserResponse;
import com.devicehive.service.UserService;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO JavaDoc
 */
@Path("/user")
public class UserController {

    @Inject
    private UserService userService;


    @GET
    @RolesAllowed("Administrator")
    @Produces(MediaType.APPLICATION_JSON)
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
        boolean sortOrderASC = true;
        if ("DESC".equals(sortOrder)) {
            sortOrderASC = false;
        }
        if (!"login".equals(sortField) && !"id".equals(sortField) && sortField != null) {  //ID??
            throw new HiveException("The sort field cannot be equal " + sortField);//maybe better to do sort field null
        }
        //TODO validation for role and status
        return userService.getList(login, loginPattern, role, status, sortField, sortOrderASC, take, skip);

    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Administrator")
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public DetailedUserResponse getUser(@PathParam("id") long id) {
        User u = userService.findUserWithNetworks(id);

        if(u==null){
            throw new NotFoundException("No such user");
        }


        DetailedUserResponse userResponse = new DetailedUserResponse();
        userResponse.setId(u.getId());
        userResponse.setLogin(u.getLogin());
        userResponse.setRole(u.getRole());
        userResponse.setStatus(u.getStatus());
        userResponse.setLastLogin(u.getLastLogin());

        Set<SimpleNetworkResponse> networks = new HashSet<>();

        for (Network n : u.getNetworks()) {
            SimpleNetworkResponse snr = new SimpleNetworkResponse();
            snr.setId(n.getId());
            snr.setDescription(n.getDescription());
            snr.setKey(n.getKey());
            snr.setName(n.getName());
            networks.add(snr);
        }

        userResponse.setNetworks(networks);

        return userResponse;
    }

    @POST
    @RolesAllowed("Administrator")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleUserResponse insertUser(UserInsert user) {
        User u = userService.findByLogin(user.getLogin());

        if (u != null) {
            throw new ForbiddenException("User with such login already exists");
        }

        u = userService.createUser(user.getLogin(), user.getRoleEnum(), user.getStatusEnum(), user.getPassword());
        SimpleUserResponse response = new SimpleUserResponse();
        response.setId(u.getId());
        response.setLogin(u.getLogin());
        response.setRole(u.getRole());
        response.setStatus(u.getStatus());
        return response;
    }


    @PUT
    @Path("/{id}")
    @RolesAllowed("Administrator")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserInsert user, @PathParam("id") long userId) {
        User u = userService.findByLogin(user.getLogin());

        if (u != null) {
            throw new ForbiddenException("User with such login already exists");
        }

        userService.updateUser(userId, user.getLogin(), user.getRoleEnum(), user.getStatusEnum(), user.getPassword());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("Administrator")
    public Response updateUser(@PathParam("id") long userId) {
        userService.deleteUser(userId);
        return Response.ok().build();
    }


    @GET
    @Path("/{id}/network/{networkId}")
    @RolesAllowed("Administrator")
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleNetworkResponse getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            User u = userService.findUserWithNetworks(id);
            for (Network n : u.getNetworks()) {
                if (n.getId() == networkId) {
                    SimpleNetworkResponse response = new SimpleNetworkResponse();
                    response.setId(n.getId());
                    response.setKey(n.getKey());
                    response.setName(n.getName());
                    response.setDescription(n.getDescription());
                    return response;
                }
            }
        } catch (Exception e) {
            throw new NotFoundException();
        }
        throw new NotFoundException();
    }


    @PUT
    @Path("/{id}/network/{networkId}")
    @RolesAllowed("Administrator")
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.assignNetwork(id, networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/network/{networkId}")
    @RolesAllowed("Administrator")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unassignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.unassignNetwork(id, networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.ok().build();
    }
}
