package com.devicehive.controller;

import com.devicehive.model.ApiInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;

@Component
@Path("/info")
@Api(tags = {"ApiInfoVO"}, description = "API information", consumes = "application/json")
public class ApiInfoController {

    @GET
    @Produces({"application/json"})
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get API info", notes = "Returns version of API, server timestamp and WebSocket base uri"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns version of API, server timestamp and WebSocket base uri",
                    response = ApiInfoVO.class),
    })
    public Response getApiInfo() {
        ApiInfoVO apiInfo = new ApiInfoVO();
        apiInfo.setApiVersion("1.0.0");
        apiInfo.setServerTimestamp(new Date());
        return Response.ok(apiInfo).build();
    }
}
