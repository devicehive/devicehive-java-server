package com.devicehive.handler.dao.list;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.rpc.ListDeviceClassRequest;
import com.devicehive.model.rpc.ListDeviceClassResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceClassVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ListDeviceClassHandler implements RequestHandler {

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Override
    public Response handle(Request request) {
        final ListDeviceClassRequest req = (ListDeviceClassRequest) request.getBody();

        final List<DeviceClassVO> deviceClasses =
                deviceClassDao.list(req.getName(), req.getNamePattern(),
                        req.getSortField(), req.getSortOrderAsc(),
                        req.getTake(), req.getSkip());

        return Response.newBuilder()
                .withBody(new ListDeviceClassResponse(deviceClasses))
                .buildSuccess();
    }
}
