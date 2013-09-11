package com.devicehive.websockets.util;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.websockets.handlers.annotations.Authorize;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Priority;
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

    private DeviceService deviceService;
    private UserService userService;

    public AuthenticationInterceptor() throws NamingException {
        InitialContext initialContext = new InitialContext();
        this.userService = (UserService) initialContext.lookup("java:comp/env/UserService");
        this.deviceService = (DeviceService) initialContext.lookup("java:comp/env/DeviceService");
    }

    @AroundInvoke
    public Object authenticate(InvocationContext ctx) throws Exception {
        ImmutablePair<JsonObject, Session> pair = ThreadLocalVariablesKeeper.getJsonAndSession();
        Device authDevice =  getDeviceAndSetToSession(pair.left, pair.right);
        User authUser = getUserAndSetToSession(pair.left, pair.right);
        HivePrincipal principal = new HivePrincipal(authUser, authDevice);
        ThreadLocalVariablesKeeper.setPrincipal(principal);
        return ctx.proceed();
    }

    private Device getDeviceAndSetToSession(JsonObject request, Session session){
        Device device = WebsocketSession.getAuthorisedDevice(session);

        String deviceId = null, deviceKey = null;
        if (request.get("deviceId") != null) {
            deviceId = request.get("deviceId").getAsString();
        }
        if (request.get("deviceKey") != null) {
            deviceKey = request.get("deviceKey").getAsString();
        }
        if (deviceId != null && deviceKey != null) {
            device = deviceService.authenticate(deviceId, deviceKey);
        }
        WebsocketSession.setAuthorisedDevice(session, device);
        return device;
    }

    private User getUserAndSetToSession(JsonObject request, Session session){
        User user = WebsocketSession.getAuthorisedUser(session);
        String login = null, password = null;
        if (request.get("login") != null) {
            login = request.get("login").getAsString();
        }
        if (request.get("password") != null) {
            password = request.get("password").getAsString();
        }
        if (login != null && password != null) {
            user = userService.authenticate(login, password);
        }
        WebsocketSession.setAuthorisedUser(session, user);
        return user;
    }
}
