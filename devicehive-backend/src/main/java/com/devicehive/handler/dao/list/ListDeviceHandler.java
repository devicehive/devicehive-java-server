package com.devicehive.handler.dao.list;

import com.devicehive.dao.DeviceDao;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListDeviceResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ListDeviceHandler implements RequestHandler {

    @Autowired
    private DeviceDao deviceDao;

    @Override
    public Response handle(Request request) {

        final ListDeviceRequest req = (ListDeviceRequest) request.getBody();

        final List<DeviceVO> devices = deviceDao.list(req.getName(), req.getNamePattern(), req.getStatus(),
                req.getNetworkId(), req.getNetworkName(), req.getDeviceClassId(), req.getDeviceClassName(),
                req.getSortField(), req.getSortOrderAsc(), req.getTake(), req.getSkip(), req.getPrincipal());

        return Response.newBuilder()
                .withBody(new ListDeviceResponse(devices))
                .buildSuccess();
    }
}
