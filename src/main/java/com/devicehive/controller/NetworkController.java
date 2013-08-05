package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.NetworkRequest;
import com.devicehive.service.NetworkService;
import com.devicehive.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;


/**
 * TODO JavaDoc
 */
@Path("/network")
public class NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Inject
    private NetworkService networkService;

    @Inject
    private UserService userService;


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
     * @param namePattern
     * @param sortField   Sort Field, can be either "id", "key", "name" or "description"
     * @param sortOrder   ASC - ascending, otherwise descending
     * @param take        limit, default 1000
     * @param skip        offset, default 0
     */
    @GET
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response getNetworkList(@QueryParam("name") String name,
                                   @QueryParam("namePattern") String namePattern,
                                   @QueryParam("sortField") String sortField,
                                   @QueryParam("sortOrder") String sortOrder,
                                   @QueryParam("take") Integer take,
                                   @QueryParam("skip") Integer skip,
                                   @Context ContainerRequestContext requestContext) {

        logger.debug("Network list requested");

        boolean sortOrderAsc = true;

        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            logger.debug("Unable to proceed network list request. Invalid sortOrder");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }

        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            logger.debug("Unable to proceed network list request. Invalid sortField");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }

        User u = userService.getCurrent(requestContext);

        if (u == null) {
            logger.debug("User is not authorized to run");
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("User is not authorized to run."));
        }

        List<Network> result = networkService.list(name, namePattern, sortField, sortOrderAsc, take, skip, u.isAdmin() ? null : u.getId());

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
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN})
    public Response getNetwork(@PathParam("id") long id) {
        logger.debug("Network get requested.");
        Network existing = networkService.getWithDevicesAndDeviceClasses(id);

        if (existing == null) {
            logger.debug("Network with id = " + id + "does not exists");
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Network not found."));
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
    public Response insert(NetworkRequest nr) {
        logger.debug("Network insert requested");
        Network n = new Network();

        //TODO: if request if malformed this code will fall with NullPointerException
        if (nr.getName() == null || nr.getName().getValue() == null) {
            logger.debug("Unable to proceed network insert. Name field is required.");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse("Invalid request parameters."));
        }
        n.setName(nr.getName().getValue());
        if (nr.getKey() != null) {
            n.setKey(nr.getKey().getValue());
        }
        if (nr.getDescription() != null) {
            n.setDescription(nr.getDescription().getValue());
        }

        Network result;

        try {
            result = networkService.insert(n);
        } catch (Exception ex) {
            logger.debug("Unable to proceed network insert.", ex);
            return ResponseFactory.response(Response.Status.FORBIDDEN, new ErrorResponse("Network couldn't be created"));
        }
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
    public Response update(NetworkRequest nr, @PathParam("id") long id) {

        logger.debug("Network update requested");
        nr.setId(id);

        Network n = networkService.getById(id);

        if (n == null) {
            logger.debug("Unable to update network. Network with id = " + id + " does not exists");
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Network not found."));
        }

        if (nr.getKey() != null) {
            n.setKey(nr.getKey().getValue());
        }

        if (nr.getName() != null) {
            n.setName(nr.getName().getValue());
        }

        if (nr.getDescription() != null) {
            n.setDescription(nr.getDescription().getValue());
        }

        networkService.update(n);
        logger.debug("Network has been updated successfully");

        return ResponseFactory.response(Response.Status.CREATED);

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
        logger.debug("Network with id = " + id + " does not exists any more.");
        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}
