package com.devicehive.handler.command;

import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class CommandInsertHandler implements RequestHandler {

    @Autowired
    private HazelcastService storageService;

    @Override
    public Response handle(Request request) {
        CommandInsertRequest commandInsertRequest = (CommandInsertRequest) request.getBody();

        storageService.store(commandInsertRequest.getDeviceCommand());

        CommandInsertResponse payload = new CommandInsertResponse(commandInsertRequest.getDeviceCommand());
        return Response.newBuilder()
                .withBody(payload)
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
    }
}
