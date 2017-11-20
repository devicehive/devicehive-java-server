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

import com.devicehive.model.query.PluginReqisterQuery;
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

import javax.ws.rs.BeanParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

@Api(tags = {"Plugin"}, description = "Plugin management operations", consumes = "application/json")
@Path("/plugin")
@Produces({"application/json"})
public interface PluginResource {

    @POST
    @Path("/register")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "Register Plugin", notes = "Registers plugin in DH Server")
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns plugin uuid, topic name and health check period",
                    response = PluginVO.class),
            @ApiResponse(code = 400, message = "healthCheckUrl doesn't respond")
    })
    void register(
            @BeanParam PluginReqisterQuery pluginReqisterQuery,
            @ApiParam(value = "Filter body", defaultValue = "{}", required = true) 
                    PluginUpdate filterToCreate,
            @ApiParam(name = "Authorization", value = "Authorization token", required = true)
            @HeaderParam("Authorization") String authorization,
            @Suspended final AsyncResponse asyncResponse);
    
}
