package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.AccessKeyDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.AccessKey;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.User;
import com.devicehive.model.UserRole;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.utils.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED;

/**
 * REST Controller for access keys: <i>/user/{userId}/accesskey</i>
 * See <a href="http://www.devicehive.com/restful/#Reference/AccessKey">DeviceHive RESTful API: AccessKey</a> for
 * details.
 */
@Path("/user/{userId}/accesskey")
@LogExecutionTime
public class AccessKeyController {
    private static Logger logger = LoggerFactory.getLogger(AccessKeyController.class);
    @EJB
    private UserService userService;
    @EJB
    private AccessKeyService accessKeyService;
    @EJB
    private AccessKeyDAO accessKeyDAO;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/list">DeviceHive RESTful API:
     * AccessKey: list</a>
     * Gets list of access keys and their permissions.
     *
     * @param userId User identifier. Use the 'current' keyword to list access keys of the current user.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/AccessKey/">AccessKey</a> resources in the response body according to the specification.
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response list(@PathParam("userId") String userId, @Context SecurityContext securityContext) {

        logger.debug("Access key : list requested for userId : {}", userId);

        Long id = getUser(securityContext, userId).getId();
        List<AccessKey> keyList = accessKeyDAO.list(id);

        logger.debug("Access key : insert proceed successfully for userId : {}", userId);

        return ResponseFactory.response(Response.Status.OK, keyList, ACCESS_KEY_LISTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/get">DeviceHive RESTful API:
     * AccessKey: get</a>
     * Gets information about access key and its permissions.
     *
     * @param userId      User identifier. Use the 'current' keyword to get access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive
     *         .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response get(@PathParam("userId") String userId, @PathParam("id") long accessKeyId,
                        @Context SecurityContext securityContext) {

        logger.debug("Access key : get requested for userId : {} and accessKeyId", userId, accessKeyId);

        Long id = getUser(securityContext, userId).getId();
        AccessKey result = accessKeyDAO.get(id, accessKeyId);
        if (result == null) {
            logger.debug("Access key : list failed for userId : {} and accessKeyId : {}. Reason: No access key found" +
                    ".", userId, accessKeyId);
            return ResponseFactory
                    .response(Response.Status.NOT_FOUND, new ErrorResponse("Access key not found."));
        }

        logger.debug("Access key : insert proceed successfully for userId : {} and accessKeyId : {}", userId, accessKeyId);

        return ResponseFactory.response(Response.Status.OK, result, ACCESS_KEY_LISTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/insert">DeviceHive RESTful API:
     * AccessKey: insert</a>
     * Creates new access key.
     *
     * @param userId User identifier. Use the 'current' keyword to get access key of the current user.
     * @return If successful, this method returns an <a href="http://www.devicehive
     *         .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    @POST
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response insert(@PathParam("userId") String userId,
                           @JsonPolicyApply(ACCESS_KEY_PUBLISHED) AccessKey key,
                           @Context SecurityContext securityContext) {

        logger.debug("Access key : insert requested for userId : {}", userId);

        User user = getUser(securityContext, userId);
        AccessKey generatedKey = accessKeyService.create(user, key);
        logger.debug("Access key : insert proceed successfully for userId : {}", userId);
        return ResponseFactory.response(Response.Status.OK, generatedKey, ACCESS_KEY_SUBMITTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/update">DeviceHive RESTful
     * API: AccessKey: update</a>
     * Updates an existing access key.
     *
     * @param userId      User identifier. Use the 'current' keyword to update access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive
     *         .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response update(@PathParam("userId") Long userId, @PathParam("id") Long accessKeyId) {
        return null;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/delete">DeviceHive RESTful
     * API: AccessKey: delete</a>
     *
     * @param userId      User identifier. Use the 'current' keyword to update access key of the current user.
     * @param accessKeyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive
     *         .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response delete(@PathParam("userId") Long userId, @PathParam("id") Long accessKeyId) {
        return null;
    }

    private User getUser(SecurityContext securityContext, String userId){
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User currentUser = principal.getUser();

        Long id;
        if (userId.equalsIgnoreCase(Constants.CURRENT_USER)){
            return currentUser;
        }
        else{
            try{
                id = Long.parseLong(userId);
            }
            catch (NumberFormatException e){
                throw new HiveException("Bad user identifier :" + userId, e,
                        Response.Status.BAD_REQUEST.getStatusCode());
            }
        }

        User result = null;
        if (!currentUser.getId().equals(id) && currentUser.getRole().equals(UserRole.ADMIN)){
            result = userService.findById(id);
            if (result == null){
                throw new HiveException("User not found", Response.Status.NOT_FOUND.getStatusCode());
            }

        }
        if (!currentUser.getId().equals(id) && currentUser.getRole().equals(UserRole.CLIENT)){
            throw new HiveException("User not found", Response.Status.NOT_FOUND.getStatusCode());
        }
        return result;
    }
}
