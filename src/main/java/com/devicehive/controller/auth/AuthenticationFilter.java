package com.devicehive.controller.auth;


import com.devicehive.dao.DeviceDAO;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.service.UserService;

import javax.annotation.Priority;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {



    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean secure = requestContext.getSecurityContext().isSecure();

        requestContext.setSecurityContext(new HiveSecurityContext(new UserPrincipal(authUser(requestContext)), new DevicePrincipal(authDevice(requestContext)), secure));
    }


    private Device authDevice(ContainerRequestContext requestContext) throws IOException {
        String deviceId = requestContext.getHeaderString("Auth-DeviceID");
        if (deviceId == null) {
            return null;

        }
        String deviceKey = requestContext.getHeaderString("Auth-DeviceKey");

        UUID uuid;
        try {
            uuid = UUID.fromString(deviceId);
            DeviceDAO deviceDAO = (DeviceDAO) new InitialContext().lookup("java:comp/env/DeviceDAO");
            return deviceDAO.findByUUIDAndKey(uuid, deviceKey);
        } catch (IllegalArgumentException ex) {
            return null;
        } catch (NamingException ex) {
            throw new IOException(ex);
        }
    }

    private User authUser(ContainerRequestContext requestContext) throws IOException {
        String login = "test"; //TODO
        String password = "test"; //TODO
        if(login == null) {
            return null;
        }

        try {
            UserService userService = (UserService) new InitialContext().lookup("java:comp/env/UserService");
            return userService.authenticate(login, password);
        } catch (IllegalArgumentException ex) {
            return null;
        } catch (NamingException ex) {
            throw new IOException(ex);
        }
    }
}


