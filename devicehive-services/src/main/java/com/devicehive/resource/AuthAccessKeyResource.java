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

import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.vo.AccessKeyVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = {"Authentication"}, description = "Auth access key operations", consumes = "application/json")
@Path("/auth/accesskey")
public interface AuthAccessKeyResource {

    @POST
    @PreAuthorize("permitAll")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login", notes = "Authenticates a user and returns a session-level access key.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "If successful, this method returns the object with the following properties in the response body.",
                    response = AccessKeyVO.class),
            @ApiResponse(code = 403, message = "If identity provider is not allowed")
    })
    Response login(
            @ApiParam(value = "Access key request", required = true)
            AccessKeyRequestVO request);

    @DELETE
    @PreAuthorize("hasRole('KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    @ApiOperation(value = "Logout",notes = "Invalidates the session-level token.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body.")
    })
    Response logout();

}
