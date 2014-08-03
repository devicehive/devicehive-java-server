package com.devicehive.auth.websockets;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.model.Device;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.handlers.annotations.WebsocketController;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.websocket.Session;

import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.DEVICE_KEY;

@WebsocketController
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuthenticationInterceptor {

    @Inject
    private DeviceService deviceService;

    @Inject
    private HiveSecurityContext hiveSecurityContext;

    @AroundInvoke
    public Object authenticate(InvocationContext ctx) throws Exception {
        Session session = ThreadLocalVariablesKeeper.getSession();
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);

        if (state.getHivePrincipal() != null && state.getHivePrincipal().isAuthenticated()) {
            hiveSecurityContext.setHivePrincipal(ObjectUtils.cloneIfPossible(state.getHivePrincipal()));
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
                    hiveSecurityContext.setHivePrincipal(principal);
                }
            }
        }
        hiveSecurityContext.setOrigin(state.getOrigin());
        hiveSecurityContext.setClientInetAddress(state.getClientInetAddress());
        return ctx.proceed();

    }

}
