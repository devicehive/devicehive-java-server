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
