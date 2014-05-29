package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.converters.SortOrder;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.service.NetworkService;
import com.devicehive.util.AsynchronousExecutor;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.auth.AllowedKeyAction.Action.GET_NETWORK;
import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.configuration.Constants.NAME_PATTERN;
import static com.devicehive.configuration.Constants.SKIP;
import static com.devicehive.configuration.Constants.SORT_FIELD;
import static com.devicehive.configuration.Constants.SORT_ORDER;
import static com.devicehive.configuration.Constants.TAKE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/network")
@LogExecutionTime
public class NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);
    @EJB
    private NetworkService networkService;
    @EJB
    private AsynchronousExecutor executor;

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
    public void getNetworkList(@QueryParam(NAME) final String name,
                               @QueryParam(NAME_PATTERN) final String namePattern,
                               @QueryParam(SORT_FIELD) final String sortField,
                               @QueryParam(SORT_ORDER) @SortOrder final Boolean sortOrder,
                               @QueryParam(TAKE) final Integer take,
                               @QueryParam(SKIP) final Integer skip,
                               @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Network list requested");

        final boolean sortOrderRes = sortOrder == null ? true : sortOrder;

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                logger.debug("Network list request proceed. Name {}, namePattern {}, sortField {}, sortOrder {}, " +
                        "take {}, skip {}", name, namePattern, sortField, sortOrder, take, skip);
            }
        });
        final HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
                        logger.debug("Unable to proceed network list request. Invalid sortField");
                        asyncResponse.resume(ResponseFactory.response(Response.Status.BAD_REQUEST,
                                new ErrorResponse(Messages.INVALID_REQUEST_PARAMETERS)));
                    }
                    final String sortFieldLower = sortField == null ? null : sortField.toLowerCase();
                    List<Network> result = networkService
                            .list(name, namePattern, sortFieldLower, sortOrderRes, take, skip, principal);
                    asyncResponse.resume(ResponseFactory
                            .response(Response.Status.OK, result, JsonPolicyDef.Policy.NETWORKS_LISTED));
                } catch (Exception e) {
                    asyncResponse.resume(e);
                }
            }
        });

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
    public Response getNetwork(@PathParam(ID) long id) {

        logger.debug("Network get requested.");
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        Network existing = networkService.getWithDevicesAndDeviceClasses(id, principal);
        if (existing == null) {
            logger.debug("Network with id =  {} does not exists", id);
            return ResponseFactory
                    .response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.NETWORK_NOT_FOUND, id)));
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
    public Response update(NetworkUpdate networkToUpdate, @PathParam(ID) long id) {

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
    public Response delete(@PathParam(ID) long id) {

        logger.debug("Network delete requested");
        networkService.delete(id);
        logger.debug("Network with id = {} does not exists any more.", id);

        return ResponseFactory.response(NO_CONTENT);
    }
}