package com.devicehive.handler.dao.list;

import com.devicehive.dao.AccessKeyDao;
import com.devicehive.model.rpc.ListAccessKeyRequest;
import com.devicehive.model.rpc.ListAccessKeyResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.AccessKeyVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ListAccessKeyHandler implements RequestHandler {

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Override
    public Response handle(Request request) {

        final ListAccessKeyRequest req = (ListAccessKeyRequest) request.getBody();

        final List<AccessKeyVO> accessKeys =
                accessKeyDao.list(req.getUserId(), req.getLabel(),
                req.getLabelPattern(), req.getType(),
                req.getSortField(), req.getSortOrderAsc(),
                req.getTake(), req.getSkip());

        return Response.newBuilder()
                .withBody(new ListAccessKeyResponse(accessKeys))
                .buildSuccess();
    }
}
