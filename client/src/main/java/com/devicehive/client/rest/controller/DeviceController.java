package com.devicehive.client.rest.controller;


import com.devicehive.client.model.Device;
import com.devicehive.client.rest.RequestFactory;
import com.devicehive.client.rest.ResponseFactory;

import javax.ws.rs.client.WebTarget;

public class DeviceController {

    public Device getDevice(String guid) {
        WebTarget target = RequestFactory.request("/device/" + guid, null);
        return ResponseFactory.responseGet(target, Device.class);
    }
}
