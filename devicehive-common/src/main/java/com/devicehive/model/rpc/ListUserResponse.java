package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;
import com.devicehive.vo.UserVO;
import java.util.List;

public class ListUserResponse extends Body {

    private List<UserVO> users;

    public ListUserResponse(List<UserVO> users) {
        super(Action.LIST_USER_RESPONSE.name());
        this.users = users;
    }

    public List<UserVO> getUsers() {
        return users;
    }
}
