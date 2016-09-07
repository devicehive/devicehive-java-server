package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;
import com.devicehive.vo.DeviceClassWithEquipmentVO;

import java.util.List;

public class ListDeviceClassResponse extends Body {

    private List<DeviceClassWithEquipmentVO> deviceClasses;

    public ListDeviceClassResponse(List<DeviceClassWithEquipmentVO> deviceClasses) {
        super(Action.LIST_DEVICE_CLASS_RESPONSE.name());
        this.deviceClasses = deviceClasses;
    }

    public List<DeviceClassWithEquipmentVO> getDeviceClasses() {
        return deviceClasses;
    }
}
