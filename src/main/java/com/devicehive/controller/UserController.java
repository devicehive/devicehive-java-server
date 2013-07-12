package com.devicehive.controller;

import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.User;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * TODO JavaDoc
 */
@Path("/user")
public class UserController {

    @Inject
    private UserDAO userDAO;

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
    ){
        boolean sortOrderASC = true;
        if ("DESC".equals(sortOrder)){
            sortOrderASC = false;
        }
        if (!"login".equals(sortField) && !"id".equals(sortField) && sortField != null){  //ID??
           throw new HiveException("The sort field cannot be equal " + sortField);//maybe better to do sort field null
        }
        //TODO validation for role and status
        List<User> result = userDAO.getList(login, loginPattern, role, status, sortField, sortOrderASC, take, skip);
        return result;

    }



    public Response getDeviceList() {
        return Response.ok().build();
    }
}
