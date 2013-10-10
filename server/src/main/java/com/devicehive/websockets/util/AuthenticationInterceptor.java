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
    private AccessKeyService accessKeyService;

    public AuthenticationInterceptor() throws NamingException {
        InitialContext initialContext = new InitialContext();
        this.userService = (UserService) initialContext.lookup("java:comp/env/UserService");
        this.deviceService = (DeviceService) initialContext.lookup("java:comp/env/DeviceService");
        this.accessKeyService = (AccessKeyService) initialContext.lookup("java:comp/env/AccessKeyService");
    }

    @AroundInvoke
    public Object authenticate(InvocationContext ctx) throws Exception {
        Session session = ThreadLocalVariablesKeeper.getSession();
        JsonObject request = ThreadLocalVariablesKeeper.getRequest();
        Device authDevice =  getDeviceAndSetToSession(request, session);
        User authUser = getUserAndSetToSession(request, session);
        AccessKey authAccessKey = getAccessKeyAndSetToSession(request, session);
        HivePrincipal principal = new HivePrincipal(authUser, authDevice, authAccessKey);
        WebsocketSession.setPrincipal(session, principal);
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

    private AccessKey getAccessKeyAndSetToSession(JsonObject request, Session session){
        AccessKey key = WebsocketSession.getAuthorizedAccessKey(session);
        String keyString = null;
        if (request.get("accessKey") != null){
            keyString = request.get("accessKey").getAsString();
        }
        if (keyString != null){
            key = accessKeyService.authenticate(keyString);
        }
        WebsocketSession.setAuthorizedKey(session, key);
        return key;
    }
}
