package com.devicehive.websockets.util;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.AccessKey;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.utils.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.handlers.annotations.Authorize;
import com.google.gson.JsonObject;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.websocket.Session;

@Authorize
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuthenticationInterceptor {

    @EJB
    private DeviceService deviceService;



    @AroundInvoke
    public Object authenticate(InvocationContext ctx) throws Exception {
        JsonObject request = ThreadLocalVariablesKeeper.getRequest();
        String deviceId = null, deviceKey = null;
        if (request.get("deviceId") != null) {
            deviceId = request.get("deviceId").getAsString();
        }
        if (request.get("deviceKey") != null) {
            deviceKey = request.get("deviceKey").getAsString();
        }
        if (deviceId != null && deviceKey != null) {
            Device device = deviceService.authenticate(deviceId, deviceKey);
            if (device != null) {
                HivePrincipal principal = new HivePrincipal(null, device, null);
                ThreadLocalVariablesKeeper.setPrincipal(principal);
            }
        }
        return ctx.proceed();
    }

}
