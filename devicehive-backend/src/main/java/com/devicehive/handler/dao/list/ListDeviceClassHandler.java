package com.devicehive.handler.dao.list;

import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.rpc.ListDeviceClassRequest;
import com.devicehive.model.rpc.ListDeviceClassResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ListDeviceClassHandler implements RequestHandler {

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Override
    public Response handle(Request request) {
        final ListDeviceClassRequest req = (ListDeviceClassRequest) request.getBody();

        final List<DeviceClassWithEquipmentVO> deviceClasses =
                deviceClassDao.list(req.getName(), req.getNamePattern(),
                        req.getSortField(), req.getSortOrderAsc(),
                        req.getTake(), req.getSkip());

        return Response.newBuilder()
                .withBody(new ListDeviceClassResponse(deviceClasses))
                .buildSuccess();
    }
}
