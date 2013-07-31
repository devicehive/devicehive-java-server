package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Network;
import com.devicehive.model.request.NetworkRequest;
import com.devicehive.service.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.List;


/**
 * TODO JavaDoc
 */
@Path("/network")
public class NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Inject
    private NetworkService networkService;


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
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetworkList(@QueryParam("name") String name,
                                        @QueryParam("namePattern") String namePattern,
                                        @QueryParam("sortField") String sortField,
                                        @QueryParam("sortOrder") String sortOrder,
                                        @QueryParam("take") Integer take,
                                        @QueryParam("skip") Integer skip) {
        boolean sortOrderAsc = true;
        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NETWORKS_LISTED)};
        List<Network> result = networkService.list(name, namePattern, sortField, sortOrderAsc, take,
                skip);
        return Response.ok().entity(result, annotations).build();
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
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetworkList(@PathParam("id") long id) {
        Network existing = networkService.getWithDevicesAndDeviceClasses(id);
        if (existing == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NETWORK_PUBLISHED)};
        return Response.ok().entity(existing, annotations).build();
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
     *
     */
    @POST
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(NetworkRequest nr) {
        Network n = new Network();
        //TODO: if request if malformed this code will fall with NullPointerException
        n.setKey(nr.getKey().getValue());
        n.setDescription(nr.getDescription().getValue());
        n.setName(nr.getName().getValue());
        Network result = networkService.insert(n);
        Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(JsonPolicyDef.Policy.NETWORK_SUBMITTED)};
        return Response.status(Response.Status.CREATED).entity(result,annotations).build();
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(NetworkRequest nr, @PathParam("id") long id) {
        nr.setId(id);
        Network n = networkService.getById(id);
        if (n == null){
            return Response.status(Response.Status.NOT_FOUND).build();
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
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Deletes network by specified id.
     * If success, outputs empty response
     *
     * @param id network's id
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(HiveRoles.ADMIN)
    public Response delete(@PathParam("id") long id) {
        if (!networkService.delete(id)){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
