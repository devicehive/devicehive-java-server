package com.devicehive.resource;

import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.vo.JwtTokenVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST controller for JwtToken.
 */
@Path("/token")
@Api(tags = {"JwtToken"}, description = "Represents an JWT access/refresh tokens management to API/device.",
        consumes = "application/json")
@Produces({"application/json"})
public interface JwtTokenResource {

    @POST
    @Consumes(APPLICATION_JSON)
    @PreAuthorize("permitAll")
    @ApiOperation(value = "JWT token request")
    @ApiResponses({
            @ApiResponse(code = 200,
                    message = "If successful, this method returns a JWT token in the response body.",
                    response = JwtTokenVO.class),
        @ApiResponse(code = 404, message = "If access token not found")
    })
    Response tokenRequest(
            @ApiParam(name = "grant_type", value = "Grant type", required = true)
            JwtPayload payload);
}


