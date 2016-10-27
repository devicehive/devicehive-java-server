package com.devicehive.resource;

import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.JwtTokenVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = {"Authentication"}, description = "OAuth2 JWT token operations", consumes = "application/json")
@Path("/oauth/token")
public interface AuthJwtTokenResource {

    @POST
    @PreAuthorize("permitAll")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login", notes = "Authenticates a user and returns a session-level JWT token.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "If successful, this method returns the object with the following properties in the response body.",
                    response = JwtTokenVO.class),
            @ApiResponse(code = 403, message = "If identity provider is not allowed")
    })
    Response login(
            @ApiParam(value = "Access key request", required = true)
            AccessKeyRequestVO request);
}
