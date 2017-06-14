package com.devicehive.resource.impl;

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

import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.HealthResource;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Provide health status
 */
@Service
public class HealthResourceImpl implements HealthResource {
    private static final Logger logger = LoggerFactory.getLogger(HealthResourceImpl.class);

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private RpcServer rpcServer;

    @Autowired
    private DataSourceHealthIndicator dataSourceHealthIndicator;

    @Override
    public Response getHealthStatus() {
        logger.debug("Start DataSource health check");
        if (dataSourceHealthIndicator.health().getStatus() != Status.UP) {
            return Response
                    .status(SERVICE_UNAVAILABLE)
                    .entity(new ErrorResponse(SERVICE_UNAVAILABLE.getStatusCode(), "Data source isn't available"))
                    .build();
        }
        logger.debug("Start Hazelcast health check");
        if (!hazelcastService.isRunning()) {
            return Response
                    .status(SERVICE_UNAVAILABLE)
                    .entity(new ErrorResponse(SERVICE_UNAVAILABLE.getStatusCode(), "Hazelcast isn't available"))
                    .build();
        }
        logger.debug("Start RpcServer health check");
        if (!rpcServer.isRunning()) {
            return Response
                    .status(SERVICE_UNAVAILABLE)
                    .entity(new ErrorResponse(SERVICE_UNAVAILABLE.getStatusCode(), "RpcServer isn't available"))
                    .build();
        }
        return Response.status(OK).build();
    }
}
