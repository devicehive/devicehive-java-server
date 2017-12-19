package com.devicehive.messages.handler.dao.count;

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

import com.devicehive.dao.UserDao;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.CountUserRequest;
import com.devicehive.model.rpc.CountUserResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CountUserHandler implements RequestHandler {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Response handle(Request request) {

        final CountUserRequest req = (CountUserRequest) request.getBody();

        final List<UserVO> users = userDao.list(req.getLogin(), req.getLoginPattern(),
                req.getRole(), req.getStatus(), null, true, null, null);

        final EntityCountResponse entityCountResponse = new EntityCountResponse(users.size());

        return Response.newBuilder()
                .withBody(new CountUserResponse(entityCountResponse))
                .buildSuccess();
    }
}
