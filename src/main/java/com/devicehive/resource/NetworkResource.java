package com.devicehive.resource;

import com.devicehive.model.Network;
import com.devicehive.model.updates.NetworkUpdate;
import com.wordnik.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Api(tags = {"network"})
@Path("/network")
public interface NetworkResource {

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
     * @param sortOrderSt ASC - ascending, otherwise descending
     * @param take        limit, default 1000
     * @param skip        offset, default 0
     */
    @GET
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_NETWORK')")
    @ApiOperation(value = "List networks", notes = "Returns list of networks")
    Response getNetworkList(
            @ApiParam(name = "name", value = "Network name")
            @QueryParam("name")
            String name,
            @ApiParam(name = "namePattern", value = "Name pattern")
            @QueryParam("namePattern")
            String namePattern,
            @ApiParam(name = "sortField", value = "Sort field")
            @QueryParam("sortField")
            String sortField,
            @ApiParam(name = "sortOrder", value = "Sort order")
            @QueryParam("sortOrder")
            String sortOrderSt,
            @ApiParam(name = "take", value = "Limit param")
            @QueryParam("take")
            Integer take,
            @ApiParam(name = "skip", value = "Skip param")
            @QueryParam("skip")
            Integer skip
    );

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
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'KEY') and hasPermission(null, 'GET_NETWORK')")
    @ApiOperation(value = "Get network", notes = "Returns network by id")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If network not found")
    })
    Response getNetwork(
            @ApiParam(name = "id", value = "Network id")
            @PathParam("id")
            long id);

    /**
     * Inserts new Network into database. Consumes next input:
     * <pre>
     *     {
     *       "key":"Network Key",
     *       "name":"Network Name",
     *       "description":"Network Description"
     *     }
     * </pre>
     * Where "key" is not required "description" is not required "name" is required <p/> In case of success will produce
     * following output:
     * <pre>
     *     {
     *      "description":"Network Description",
     *      "id":1,
     *      "key":"Network Key",
     *      "name":"Network Name"
     *     }
     * </pre>
     * Where "description" and "key" will be provided, if they are specified in request. Fields "id" and "name" will be
     * provided anyway.
     */
    @POST
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_NETWORK')")
    @ApiOperation(value = "Create network", notes = "Creates new network")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 403, message = "If network already exist or principal doesn't have permissions")
    })
    Response insert(
            @ApiParam(value = "Network body", defaultValue = "{}", required = true)
            Network network);

    /**
     * This method updates network with given Id. Consumes following input:
     * <pre>
     *     {
     *       "key":"Network Key",
     *       "name":"Network Name",
     *       "description":"Network Description"
     *     }
     * </pre>
     * Where "key" is not required "description" is not required "name" is not required Fields, that are not specified
     * will stay unchanged Method will produce following output:
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
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_NETWORK')")
    @ApiOperation(value = "Update network", notes = "Update existing network")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If network not found")
    })
    Response update(
            @ApiParam(value = "Network body", defaultValue = "{}", required = true)
            NetworkUpdate networkToUpdate,
            @ApiParam(name = "id", value = "Network id", required = true)
            @PathParam("id")
            long id);

    /**
     * Deletes network by specified id. If success, outputs empty response
     *
     * @param id network's id
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_NETWORK')")
    @ApiOperation(value = "Delete network", notes = "Deletes network")
    Response delete(
            @ApiParam(name = "id", value = "Network id", required = true)
            @PathParam("id")
            long id);

}
