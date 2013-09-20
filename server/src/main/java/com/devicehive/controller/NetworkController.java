package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.NetworkService;
import com.devicehive.service.UserService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.devicehive.auth.AllowedKeyAction.Action.GET_NETWORK;

@Path("/network")
@LogExecutionTime
public class NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);
    private NetworkService networkService;
    private UserService userService;

    @EJB
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Produces following output:
     * <pre>
     * [
     *  {
     *    "description":"Network Description",
     *    "id":1,
     *    "key":"Network Key",
     *    "name":"Network Name"
     *   },
     *   {
     *    "description":"Network Description",
     *    "id":2,
     *    "key":"Network Key",
     *    "name":"Network Name"
     *   }
     * ]
     * </pre>
     *
     * @param name        exact network's name, ignored, when  namePattern is not null
     * @param namePattern name pattern
     * @param sortField   Sort Field, can be either "id", "key", "name" or "description"
     * @param sortOrder   ASC - ascending, otherwise descending
     * @param take        limit, default 1000
     * @param skip        offset, default 0
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_NETWORK})
    public Response getNetworkList(@QueryParam("name") String name,
                                   @QueryParam("namePattern") String namePattern,
                                   @QueryParam("sortField") String sortField,
                                   @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
                                   @QueryParam("take") Integer take,
                                   @QueryParam("skip") Integer skip,
                                   @Context SecurityContext securityContext) {

        logger.debug("Network list requested");

        if (sortOrder == null) {
            sortOrder = true;
        }

        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            logger.debug("Unable to proceed network list request. Invalid sortField");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        User u = userService.findUserWithNetworks(user.getId());

        Set<Long> allowedNetworkIds = null;
        if (principal.getKey() != null) {
            allowedNetworkIds = new HashSet<>();
            Set<AccessKeyPermission> permissions = principal.getKey().getPermissions();
            for (AccessKeyPermission currentPermission : permissions) {
                if (currentPermission.getNetworkIdsAsSet() == null) {
                    allowedNetworkIds.add(null);
                } else {
                    allowedNetworkIds.addAll(currentPermission.getNetworkIdsAsSet());
                }
            }
        }
        List<Network> result = networkService
                .list(name, namePattern, sortField, sortOrder, take, skip, u.isAdmin() ? null : u.getId(),
                        allowedNetworkIds);


        logger.debug("Network list request proceed successfully.");
        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.NETWORKS_LISTED);
    }

    /**
     * Generates  JSON similar to this:
     * <pre>
     *     {
     *      "description":"Network Description",
     *      "id":1,
     *      "key":"Network Key",
     *      "name":"Network Name"
     *     }
     * </pre>
     *
     * @param id network id, can't be null
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_NETWORK})
    public Response getNetwork(@PathParam("id") long id, @Context SecurityContext securityContext) {

        logger.debug("Network get requested.");
        HivePrincipal principal = (HivePrincipal) securityContext.getUserPrincipal();
        User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();
        if (principal.getKey() != null) {
            Set<AccessKeyPermission> permissions = principal.getKey().getPermissions();
            Set<Long> allowedNetworksIds = new HashSet<>();
            for (AccessKeyPermission currentPermission : permissions) {
                if (currentPermission.getNetworkIdsAsSet() == null) {
                    allowedNetworksIds.add(null);
                } else {
                    allowedNetworksIds.addAll(currentPermission.getNetworkIdsAsSet());
                }
            }
            if (!allowedNetworksIds.contains(null) && !allowedNetworksIds.contains(id)) {
                logger.debug("Access key have no permissions for network with id {}", id);
                return ResponseFactory
                        .response(Response.Status.NOT_FOUND,
                                new ErrorResponse(ErrorResponse.NETWORK_NOT_FOUND_MESSAGE));
            }
        }

        Network existing =
                networkService.getWithDevicesAndDeviceClasses(id, userService.findUserWithNetworks(user.getId()));

        if (existing == null) {
            logger.debug("Network with id =  {} does not exists", id);
            return ResponseFactory
                    .response(Response.Status.NOT_FOUND, new ErrorResponse(ErrorResponse.NETWORK_NOT_FOUND_MESSAGE));
        }
        logger.debug("Network get proceed successfully.");

        return ResponseFactory.response(Response.Status.OK, existing, JsonPolicyDef.Policy.NETWORK_PUBLISHED);
    }

    /**
     * Inserts new Network into database. Consumes next input:
     * <pre>
     *     {
     *       "key":"Network Key",
     *       "name":"Network Name",
     *       "description":"Network Description"
     *     }
     * </pre>
     * Where
     * "key" is not required
     * "description" is not required
     * "name" is required
     * <p/>
     * In case of success will produce following output:
     * <pre>
     *     {
     *      "description":"Network Description",
     *      "id":1,
     *      "key":"Network Key",
     *      "name":"Network Name"
     *     }
     * </pre>
     * Where "description" and "key" will be provided, if they are specified in request.
     * Fields "id" and "name" will be provided anyway.
     */
    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    public Response insert(Network network) {
        logger.debug("Network insert requested");
        Network result = networkService.create(network);
        logger.debug("New network has been created");
        return ResponseFactory.response(Response.Status.CREATED, result, JsonPolicyDef.Policy.NETWORK_SUBMITTED);
    }

    /**
     * This method updates network with given Id. Consumes following input:
     * <pre>
     *     {
     *       "key":"Network Key",
     *       "name":"Network Name",
     *       "description":"Network Description"
     *     }
     * </pre>
     * Where
     * "key" is not required
     * "description" is not required
     * "name" is not required
     * Fields, that are not specified will stay unchanged
     * Method will produce following output:
     * <pre>
     *     {
     *      "description":"Network Description",
     *      "id":1,
     *      "key":"Network Key",
     *      "name":"Network Name"
     *     }
     * </pre>
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response update(NetworkUpdate networkToUpdate, @PathParam("id") long id) {

        logger.debug("Network update requested. Id : {}", id);
        networkService.update(id, networkToUpdate);
        logger.debug("Network has been updated successfully. Id : {}", id);
        return ResponseFactory.response(Response.Status.NO_CONTENT);

    }

    /**
     * Deletes network by specified id.
     * If success, outputs empty response
     *
     * @param id network's id
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response delete(@PathParam("id") long id) {

        logger.debug("Network delete requested");
        networkService.delete(id);
        logger.debug("Network with id = {} does not exists any more.", id);

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}
