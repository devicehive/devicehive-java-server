package com.devicehive.auth;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.service.UserService;
import org.apache.commons.codec.binary.Base64;

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
        requestContext.setSecurityContext(
                new HiveSecurityContext(
                        new HivePrincipal(authUser(requestContext),authDevice(requestContext)),secure));
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
        String auth = requestContext.getHeaders().getFirst("authorization");
        if (auth == null) {
            return null;
        }

        String decodedAuth = new String(Base64.decodeBase64(auth.substring(5).trim()));
        int pos = decodedAuth.indexOf(":");
        if (pos <= 0) {
            return null;
        }

        String login = decodedAuth.substring(0, pos);
        String password = decodedAuth.substring(pos + 1);

        try {
            // TODO: Should we really do JNDI lookup here?
            UserService userService = (UserService) new InitialContext().lookup("java:comp/env/UserService");
            return userService.authenticate(login, password);
        } catch (IllegalArgumentException ex) {
            return null;
        } catch (NamingException ex) {
            throw new IOException(ex);
        }
    }
}


