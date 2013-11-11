package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.NetworkService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

import static com.devicehive.auth.AllowedKeyAction.Action.GET_NETWORK;
import static javax.ws.rs.core.Response.Status.*;

@Path("/network")
@LogExecutionTime
public class NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);
    private NetworkService networkService;
    private AccessKeyService accessKeyService;
    private DeviceService deviceService;

    @EJB
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @EJB
    public void setAccessKeyService(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

    @EJB
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
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
                                   @QueryParam("skip") Integer skip) {

        logger.debug("Network list requested");

        if (sortOrder == null) {
            sortOrder = true;
        }

        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            logger.debug("Unable to proceed network list request. Invalid sortField");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User user = principal.getUser() != null ? principal.getUser() : principal.getKey().getUser();

        Collection<AccessKeyBasedFilterForDevices> extraFilters = principal.getKey() != null
                ? AccessKeyBasedFilterForDevices.createExtraFilters(principal.getKey().getPermissions())
                : null;
        List<Network> result = networkService
                .list(name, namePattern, sortField, sortOrder, take, skip, user, extraFilters);

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
    public Response getNetwork(@PathParam("id") long id) {

        logger.debug("Network get requested.");
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Network existing = networkService.getWithDevicesAndDeviceClasses(id, principal);
        if (existing == null) {
            logger.debug("Network with id =  {} does not exists", id);
            return ResponseFactory
                    .response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), ErrorResponse
                            .NETWORK_NOT_FOUND_MESSAGE));
        }
        return ResponseFactory.response(OK, existing, JsonPolicyDef.Policy.NETWORK_PUBLISHED);
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
        return ResponseFactory.response(CREATED, result, JsonPolicyDef.Policy.NETWORK_SUBMITTED);
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
        return ResponseFactory.response(NO_CONTENT);

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

        return ResponseFactory.response(NO_CONTENT);
    }
}
