package com.devicehive.resource;

import com.devicehive.model.ApiInfoVO;
import com.devicehive.vo.ApiConfigVO;
import com.devicehive.vo.ClusterConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Api(tags = {"ApiInfoVO"}, description = "API information", consumes = "application/json")
@Path("/info")
@Produces({"application/json"})
public interface ApiInfoResource {

    @GET
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get API info", notes = "Returns version of API, server timestamp and WebSocket base uri"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns version of API, server timestamp and WebSocket base uri",
                    response = ApiInfoVO.class),
    })
    Response getApiInfo(@Context UriInfo uriInfo);

    @GET
    @Path("/config/auth")
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get oAuth configuration", notes = "Gets information about supported authentication providers.",
            response = ApiConfigVO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns configured identity providers",
                    response = ApiConfigVO.class)
    })
    @Deprecated
    Response getOauth2Config();

    @GET
    @Path("/config/cluster")
    @PreAuthorize("permitAll")
    @ApiOperation(value = "Get cluster configuration", notes = "Returns information about cluster (Kafka, Zookeeper etc.)",
            response = ClusterConfigVO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns information about cluster (Kafka, Zookeeper etc.)",
                    response = ClusterConfigVO.class)
    })
    Response getClusterConfig();
}
