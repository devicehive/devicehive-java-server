package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;
import com.devicehive.vo.DeviceVO;

import java.util.List;

public class ListDeviceResponse extends Body {

    private List<DeviceVO> devices;

    public ListDeviceResponse(List<DeviceVO> devices) {
        super(Action.LIST_DEVICE_RESPONSE.name());
        this.devices = devices;
    }

    public List<DeviceVO> getDevices() {
        return devices;
    }
}
