package com.devicehive.auth.websockets;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.model.Device;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.websocket.Session;

import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.DEVICE_KEY;

@Aspect
@Component
@Order(0)
public class AuthenticationInterceptor {

    @Autowired
    private DeviceService deviceService;

    @Before("execution(public * com.devicehive.websockets.handlers.WebsocketHandlers+.*(..))")
    public void authenticate() throws Exception {
        Session session = ThreadLocalVariablesKeeper.getSession();
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);

        if (state.getHivePrincipal() != null && state.getHivePrincipal().isAuthenticated()) {
//            hiveAuthentication.setHivePrincipal(ObjectUtils.cloneIfPossible(state.getHivePrincipal()));
        } else {
            JsonObject request = ThreadLocalVariablesKeeper.getRequest();
            String deviceId = null, deviceKey = null;
            if (request.get(DEVICE_ID) != null) {
                deviceId = request.get(DEVICE_ID).getAsString();
            }
            if (request.get(DEVICE_KEY) != null) {
                deviceKey = request.get(DEVICE_KEY).getAsString();
            }
            if (deviceId != null && deviceKey != null) {
                Device device = deviceService.authenticate(deviceId, deviceKey);
                if (device != null) {
                    HivePrincipal principal = new HivePrincipal(null, device, null);
//                    hiveAuthentication.setHivePrincipal(principal);
                }
            }
        }
//        hiveAuthentication.setOrigin(state.getOrigin());
//        hiveAuthentication.setClientInetAddress(state.getClientInetAddress());

    }

}
