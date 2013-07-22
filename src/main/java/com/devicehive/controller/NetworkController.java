package com.devicehive.controller;

import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.request.NetworkInsert;
import com.devicehive.model.response.SimpleNetworkResponse;
import com.devicehive.service.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    @RolesAllowed("Administrator")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<SimpleNetworkResponse> getNetworkList(@QueryParam("name") String name, @QueryParam("namePattern") String namePattern,
                                                     @QueryParam("sortField") String sortField, @QueryParam("sortOrder") String sortOrder,
                                                     @QueryParam("take") Integer take, @QueryParam("skip") Integer skip) {

        List<Network> networks = networkService.list(name, namePattern, sortField, "ASC".equals(sortOrder), take, skip);
        Set<SimpleNetworkResponse> response = new HashSet<>();

        for (Network n : networks) {
            SimpleNetworkResponse rn = new SimpleNetworkResponse();
            rn.setId(n.getId());
            rn.setKey(n.getKey());
            rn.setName(n.getName());
            rn.setDescription(n.getDescription());
            response.add(rn);
        }

        return response;
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
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleNetworkResponse getNetworkList(@PathParam("id") long id) {
        Network n = networkService.getById(id);
        SimpleNetworkResponse rn = new SimpleNetworkResponse();
        rn.setId(n.getId());
        rn.setKey(n.getKey());
        rn.setName(n.getName());
        rn.setDescription(n.getDescription());
        return rn;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SimpleNetworkResponse insert(NetworkInsert nr) {
        Network n = new Network();
        n.setKey(nr.getKey());
        n.setDescription(nr.getDescription());
        n.setName(nr.getName());
        n = networkService.insert(n);

        SimpleNetworkResponse r = new SimpleNetworkResponse();
        r.setId(n.getId());
        r.setKey(n.getKey());
        r.setName(n.getName());
        r.setDescription(n.getDescription());

        return r;
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
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleNetworkResponse update(NetworkInsert nr, @PathParam("id") long id) {
        Network n = networkService.getById(id);
        if (nr.getKey() != null) {
            n.setKey(nr.getKey());
        }
        if (nr.getDescription() != null) {
            n.setDescription(nr.getDescription());
        }
        if (nr.getName() != null) {
            n.setName(nr.getName());
        }
        n = networkService.insert(n);

        SimpleNetworkResponse r = new SimpleNetworkResponse();
        r.setId(n.getId());
        r.setKey(n.getKey());
        r.setName(n.getName());
        r.setDescription(n.getDescription());

        return r;
    }

    /**
     * Deletes network by specified id.
     * If success, outputs empty response
     * @param id network's id
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") long id) {
        networkService.delete(id);
        return Response.ok().build();
    }
}
