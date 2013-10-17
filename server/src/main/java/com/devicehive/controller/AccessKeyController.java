package com.devicehive.controller;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.model.AccessKey;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.User;
import com.devicehive.model.UserRole;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST Controller for access keys: <i>/user/{userId}/accesskey</i>
 * See <a href="http://www.devicehive.com/restful/#Reference/AccessKey">DeviceHive RESTful API: AccessKey</a> for
 * details.
 */
@Path("/user/{userId}/accesskey")
@LogExecutionTime
public class AccessKeyController {
    private static Logger logger = LoggerFactory.getLogger(AccessKeyController.class);
    private UserService userService;
    private AccessKeyService accessKeyService;

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @EJB
    public void setAccessKeyService(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }


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
    public Response list(@PathParam("userId") String userId) {

        logger.debug("Access key : list requested for userId : {}", userId);

        Long id = getUser(userId).getId();
        List<AccessKey> keyList = accessKeyService.list(id);

        logger.debug("Access key : insert proceed successfully for userId : {}", userId);

        return ResponseFactory.response(OK, keyList, ACCESS_KEY_LISTED);
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
    public Response get(@PathParam("userId") String userId, @PathParam("id") long accessKeyId) {

        logger.debug("Access key : get requested for userId : {} and accessKeyId", userId, accessKeyId);

        Long id = getUser(userId).getId();
        AccessKey result = accessKeyService.get(id, accessKeyId);
        if (result == null) {
            logger.debug("Access key : list failed for userId : {} and accessKeyId : {}. Reason: No access key found" +
                    ".", userId, accessKeyId);
            return ResponseFactory
                    .response(NOT_FOUND,
                            new ErrorResponse(NOT_FOUND.getStatusCode(),"Access key not found."));
        }

        logger.debug("Access key : insert proceed successfully for userId : {} and accessKeyId : {}", userId,
                accessKeyId);

        return ResponseFactory.response(OK, result, ACCESS_KEY_LISTED);
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
                           @JsonPolicyApply(ACCESS_KEY_PUBLISHED) AccessKey key) {

        logger.debug("Access key : insert requested for userId : {}", userId);
        User user = getUser(userId);
        AccessKey generatedKey = accessKeyService.create(user, key);
        logger.debug("Access key : insert proceed successfully for userId : {}", userId);
        return ResponseFactory.response(CREATED, generatedKey, ACCESS_KEY_SUBMITTED);
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
    public Response update(@PathParam("userId") String userId, @PathParam("id") Long accessKeyId,
                           @JsonPolicyApply(ACCESS_KEY_PUBLISHED) AccessKeyUpdate accessKeyUpdate) {
        logger.debug("Access key : update requested for userId : {}, access key id : {}, access key : {} ", userId,
                accessKeyId, accessKeyUpdate);

        Long id = getUser(userId).getId();
        if (!accessKeyService.update(id, accessKeyId, accessKeyUpdate)) {
            logger.debug("Access key : update failed for userId : {} and accessKeyId : {}. Reason: No access key " +
                    "found.", userId, accessKeyId);
            return ResponseFactory
                    .response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), "Access key not found."));
        }

        logger.debug("Access key : update proceed successfully for userId : {}, access key id : {}, access key : {} ",
                userId, accessKeyId, accessKeyUpdate);
        return ResponseFactory.response(NO_CONTENT);
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
    public Response delete(@PathParam("userId") String userId, @PathParam("id") Long accessKeyId) {
        logger.debug("Access key : delete requested for userId : {}", userId);

        Long id = getUser(userId).getId();
        accessKeyService.delete(id, accessKeyId);

        logger.debug("Access key : delete proceed successfully for userId : {} and access key id : {}", userId,
                accessKeyId);
        return ResponseFactory.response(NO_CONTENT);

    }

    private User getUser(String userId) {
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User currentUser = principal.getUser();

        Long id;
        if (userId.equalsIgnoreCase(Constants.CURRENT_USER)) {
            return currentUser;
        } else {
            try {
                id = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                throw new HiveException("Bad user identifier :" + userId, e,BAD_REQUEST.getStatusCode());
            }
        }

        User result;
        if (!currentUser.getId().equals(id) && currentUser.isAdmin()) {
            result = userService.findById(id);
            if (result == null) {
                throw new HiveException("Not authorized!",UNAUTHORIZED.getStatusCode());
            }
            return result;

        }
        if (!currentUser.getId().equals(id) && currentUser.getRole().equals(UserRole.CLIENT)) {
            throw new HiveException("User not found", UNAUTHORIZED.getStatusCode());
        }
        result = currentUser;
        return result;
    }
}
