package com.devicehive.resource;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.model.query.PluginUpdateQuery;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.vo.PluginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Api(tags = {"Plugin"}, description = "Plugin management operations", consumes = "application/json")
@Path("/plugin")
@Produces({"application/json"})
public interface PluginResource {

    @GET
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "List plugins", notes = "Gets list of plugins.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns array of Plugin resources in the response body.",
                    response = PluginVO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void list(
            @ApiParam(name = "name", value = "Filter by plugin name.")
            @QueryParam("name")
                    String name,
            @ApiParam(name = "namePattern", value = "Filter by plugin name pattern. In pattern wildcards '%' and '_' can be used.")
            @QueryParam("namePattern")
                    String namePattern,
            @ApiParam(name = "topicName", value = "Filter by plugin topic name.")
            @QueryParam("topicName")
                    String topicName,
            @ApiParam(name = "status", value = "Filter by plugin status.")
            @QueryParam("status")
                    Integer status,
            @ApiParam(name = "userId", value = "Filter by associated user identifier. Only admin can see other users' plugins.")
            @QueryParam("userId")
                    Long userId,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "Id,Name")
            @QueryParam("sortField")
                    String sortField,
            @ApiParam(name = "sortOrder", value = "Result list sort order. The sortField should be specified.", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
                    String sortOrderSt,
            @ApiParam(name = "take", value = "Number of records to take from the result list.", defaultValue = "20")
            @QueryParam("take")
            @Min(0) @Max(Integer.MAX_VALUE)
                    Integer take,
            @ApiParam(name = "skip", value = "Number of records to skip from the result list.", defaultValue = "0")
            @QueryParam("skip")
            @Min(0) @Max(Integer.MAX_VALUE)
                    Integer skip,
            @Suspended final AsyncResponse asyncResponse);

    @GET
    @Path("/count")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "Count plugins", notes = "Gets count of plugins.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns the count of plugins in the response body.",
                    response = EntityCountResponse.class, responseContainer = "Count"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void count(
            @ApiParam(name = "name", value = "Filter by plugin name.")
            @QueryParam("name")
                    String name,
            @ApiParam(name = "namePattern", value = "Filter by plugin name pattern. In pattern wildcards '%' and '_' can be used.")
            @QueryParam("namePattern")
                    String namePattern,
            @ApiParam(name = "topicName", value = "Filter by plugin topic name.")
            @QueryParam("topicName")
                    String topicName,
            @ApiParam(name = "status", value = "Filter by plugin status.")
            @QueryParam("status")
                    Integer status,
            @ApiParam(name = "userId", value = "Filter by associated user identifier. Only admin can see other users' plugins.")
            @QueryParam("userId")
                    Long userId,
            @Suspended final AsyncResponse asyncResponse);

    @POST
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "Register Plugin", notes = "Registers plugin in DH Server")
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns plugin uuid, topic name and health check period",
                    response = PluginVO.class),
    })
    void register(
            @BeanParam
                    PluginReqisterQuery pluginReqisterQuery,
            @ApiParam(value = "Filter body", defaultValue = "{}", required = true) 
                    PluginUpdate filterToCreate,
            @ApiParam(name = "Authorization", value = "Authorization token", required = true)
            @HeaderParam("Authorization")
                    String authorization,
            @Suspended final AsyncResponse asyncResponse);

    @PUT
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "Update Plugin", notes = "Updates plugin in DH Server")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "If principal does not have access to the plugin"),
            @ApiResponse(code = 404, message = "If plugin not found")
    })
    void update(
            @ApiParam(name = "topicName", value = "Name of topic that was created for the plugin", required = true)
            @QueryParam("topicName")
                    String topicName,
            @BeanParam
                    PluginUpdateQuery updateQuery,
            @ApiParam(name = "Authorization", value = "Authorization token", required = true)
            @HeaderParam("Authorization")
                    String authorization,
            @Suspended final AsyncResponse asyncResponse);

    @DELETE
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "Delete Plugin", notes = "Deletes plugin in DH Server")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "If principal does not have access to the plugin"),
            @ApiResponse(code = 404, message = "If plugin not found")
    })
    void delete(
            @QueryParam("topicName")
                    String topicName,
            @ApiParam(name = "Authorization", value = "Authorization token", required = true)
            @HeaderParam("Authorization")
                    String authorization,
            @Suspended final AsyncResponse asyncResponse);
}
