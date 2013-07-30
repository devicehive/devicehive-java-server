package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Network;
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
     * @param name exact network's name, ignored, when  namePattern is not null
     * @param namePattern
     * @param sortField Sort Field, can be either "id", "key", "name" or "description"
     * @param sortOrder ASC - ascending, otherwise descending
     * @param take limit, default 1000
     * @param skip offset, default 0
     */
    @GET
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.NETWORKS_LISTED)
    public List<Network> getNetworkList( @QueryParam("name") String name,
                                         @QueryParam("namePattern") String namePattern,
                                         @QueryParam("sortField") String sortField,
                                         @QueryParam("sortOrder") String sortOrder,
                                         @QueryParam("take") Integer take,
                                         @QueryParam("skip") Integer skip) {

        return networkService.list(name, namePattern, sortField, "ASC".equals(sortOrder), take, skip);
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
     * @param id network id, can't be null
     */
    @GET
    @Path("/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.NETWORK_PUBLISHED)
    public Network getNetworkList(@PathParam("id") long id) {
        return networkService.getWithDevicesAndDeviceClasses(id);
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
     *
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
    public Response insert(Network nr) {
        Network n = new Network();
        n.setKey(nr.getKey());
        n.setDescription(nr.getDescription());
        n.setName(nr.getName());
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
    public Response update(Network nr, @PathParam("id") long id) {
        nr.setId(id);
        networkService.update(nr);
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Deletes network by specified id.
     * If success, outputs empty response
     * @param id network's id
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(HiveRoles.ADMIN)
    public Response delete(@PathParam("id") long id) {
        networkService.delete(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
