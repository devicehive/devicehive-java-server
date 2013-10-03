package com.devicehive.client.rest.controller;

import com.devicehive.client.json.strategies.JsonPolicyApply.JsonPolicyApplyLiteral;
import com.devicehive.client.model.Device;
import com.devicehive.client.rest.RequestFactory;
import com.devicehive.client.rest.ResponseFactory;

import javax.ws.rs.client.WebTarget;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;

public class DeviceController {

    public Device getDevice(String guid) {
        WebTarget target = RequestFactory.request("/device/" + guid, null);
        return ResponseFactory.responseGet(target, Device.class, new JsonPolicyApplyLiteral(DEVICE_PUBLISHED));
    }
}
