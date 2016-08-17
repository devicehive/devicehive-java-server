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

import com.devicehive.model.updates.OAuthClientUpdate;
import com.devicehive.vo.OAuthClientVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Api(tags = {"OAuthClient"})
@Path("/oauth/client")
public interface OAuthClientResource {

    @GET
    @PreAuthorize("permitAll")
    @ApiOperation(value = "List oAuth clients")
    Response list(
            @ApiParam(name = "name", value = "oAuth client name")
            @QueryParam("name")
            String name,
            @ApiParam(name = "namePattern", value = "Name pattern")
            @QueryParam("namePattern")
            String namePattern,
            @ApiParam(name = "domain", value = "oAuth client domain")
            @QueryParam("domain")
            String domain,
            @ApiParam(name = "oauthId", value = "oAuth client id")
            @QueryParam("oauthId")
            String oauthId,
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
            @QueryParam("skip") Integer skip);

    @GET
    @Path("/{id}")
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get oAuth client", notes = "Returns oAuth client by id")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If oAuth client not found")
    })
    Response get(
            @ApiParam(name = "id", value = "oAuth client id", required = true)
            @PathParam("id")
            long clientId);

    @POST
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    @ApiOperation(value = "Create oAuth client")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If request is malformed")
    })
    Response insert(
            @ApiParam(value = "oAuth client body")
            OAuthClientVO clientToInsert);

    @PUT
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    @ApiOperation(value = "Update oAuth client")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If oAuth client not found"),
            @ApiResponse(code = 403, message = "If oAuth already exist")
    })
    Response update(
            @ApiParam(name = "id", value = "oAuth client id", required = true)
            @PathParam("id")
            Long clientId,
            @ApiParam(value = "oAuth client body")
            OAuthClientUpdate clientToUpdate);

    @DELETE
    @Path("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KEY') and hasPermission(null, 'MANAGE_OAUTH_CLIENT')")
    @ApiOperation(value = "Delete oAuth client")
    Response delete(
            @ApiParam(name = "id", value = "oAuth client id", required = true)
            @PathParam("id")
            Long clientId);
}
