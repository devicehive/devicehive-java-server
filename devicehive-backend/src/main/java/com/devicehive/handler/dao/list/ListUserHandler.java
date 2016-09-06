package com.devicehive.handler.dao.list;

import com.devicehive.dao.UserDao;
import com.devicehive.model.rpc.ListUserRequest;
import com.devicehive.model.rpc.ListUserResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ListUserHandler implements RequestHandler {

    @Autowired
    private UserDao userDao;

    @Override
    public Response handle(Request request) {
        final ListUserRequest req = (ListUserRequest) request.getBody();

        final List<UserVO> users =
                userDao.list(req.getLogin(), req.getLoginPattern(),
                        req.getRole(), req.getStatus(),
                        req.getSortField(), req.getSortOrderAsc(),
                        req.getTake(), req.getSkip());

        return Response.newBuilder()
                .withBody(new ListUserResponse(users))
                .buildSuccess();
    }
}
