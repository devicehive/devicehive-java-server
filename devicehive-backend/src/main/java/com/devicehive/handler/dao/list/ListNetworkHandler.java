package com.devicehive.handler.dao.list;

import com.devicehive.dao.NetworkDao;
import com.devicehive.model.rpc.ListNetworkRequest;
import com.devicehive.model.rpc.ListNetworkResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.NetworkVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ListNetworkHandler implements RequestHandler {

    @Autowired
    private NetworkDao networkDao;

    @Override
    public Response handle(Request request) {
        final ListNetworkRequest req = (ListNetworkRequest) request.getBody();

        final List<NetworkVO> networks = networkDao.list(req.getName(), req.getNamePattern(),
                req.getSortField(), req.getSortOrderAsc(), req.getTake(), req.getSkip(), req.getPrincipal());

        return Response.newBuilder()
                .withBody(new ListNetworkResponse(networks))
                .buildSuccess();
    }
}
