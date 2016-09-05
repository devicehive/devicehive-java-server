package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;
import com.devicehive.vo.AccessKeyVO;

import java.util.List;

public class ListAccessKeyResponse extends Body {

    private List<AccessKeyVO> accessKeys;

    public ListAccessKeyResponse(List<AccessKeyVO> accessKeys) {
        super(Action.LIST_ACCESS_KEY_RESPONSE.name());
        this.accessKeys = accessKeys;
    }

    public List<AccessKeyVO> getAccessKeys() {
        return accessKeys;
    }
}
