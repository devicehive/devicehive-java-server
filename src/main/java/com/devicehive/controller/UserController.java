package com.devicehive.controller;

import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.UserInsert;
import com.devicehive.model.response.DetailedUserResponse;
import com.devicehive.model.response.SimpleNetworkResponse;
import com.devicehive.service.UserService;

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
    private UserDAO userDAO;

    @Inject
    private UserService userService;


    @GET
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
        return userDAO.getList(login, loginPattern, role, status, sortField, sortOrderASC, take, skip);

    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.USER_PUBLISHED)
    public User getUser(@PathParam("id") long id) {
        try {
            User u = userDAO.findUserWithNetworks(id);
            return u;
        } catch (Error e) {
            throw new NotFoundException();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertUser(UserInsert user) {
        User u = userDAO.findByLogin(user.getLogin());
        if (u != null) {
            throw new ForbiddenException("User with such login already exists");
        }
        userService.createUser(user.getLogin(), user.getRoleEnum(), user.getStatusEnum(), user.getPassword());
        return Response.ok().build();
    }


    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(UserInsert user, @PathParam("id") long userId) {
        //TODO: fix hql
        User u = userDAO.findByLogin(user.getLogin());

        if (u != null) {
            throw new ForbiddenException("User with such login already exists");
        }

        userService.updateUser(userId, user.getLogin(), user.getRoleEnum(), user.getStatusEnum(), user.getPassword());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response updateUser(@PathParam("id") long userId) {
        userService.deleteUser(userId);
        return Response.ok().build();
    }


     @GET
     @Path("/{id}/network/{networkId}")
     @Produces(MediaType.APPLICATION_JSON)
     public SimpleNetworkResponse getNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            User u = userDAO.findUserWithNetworks(id);
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.assignNetwork(id,networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/network/{networkId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unassignNetwork(@PathParam("id") long id, @PathParam("networkId") long networkId) {
        try {
            userService.unassignNetwork(id,networkId);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return Response.ok().build();
    }




    public Response getDeviceList() {
        return Response.ok().build();
    }
}
