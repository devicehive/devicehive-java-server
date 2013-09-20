package com.devicehive.auth;

import com.devicehive.model.AccessKey;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.utils.ThreadLocalVariablesKeeper;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.Priority;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private DeviceService deviceService;
    private UserService userService;
    private AccessKeyService accessKeyService;


    public AuthenticationFilter() throws NamingException {
        InitialContext initialContext = new InitialContext();
        this.userService = (UserService) initialContext.lookup("java:comp/env/UserService");
        this.deviceService = (DeviceService) initialContext.lookup("java:comp/env/DeviceService");
        this.accessKeyService = (AccessKeyService) initialContext.lookup("java:comp/env/AccessKeyService");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean secure = requestContext.getSecurityContext().isSecure();
        HivePrincipal principal = new HivePrincipal(authUser(requestContext), authDevice(requestContext),
                authKey(requestContext));
        ThreadLocalVariablesKeeper.setPrincipal(principal);
        requestContext.setSecurityContext(
                new HiveSecurityContext(principal, secure));
    }

    private Device authDevice(ContainerRequestContext requestContext) throws IOException {
        String deviceId = requestContext.getHeaderString("Auth-DeviceID");
        if (deviceId == null) {
            return null;
        }
        String deviceKey = requestContext.getHeaderString("Auth-DeviceKey");
        return deviceService.authenticate(deviceId, deviceKey);

    }

    private User authUser(ContainerRequestContext requestContext) throws IOException {
        String auth = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.substring(0, 5).equalsIgnoreCase("Basic")) {
            String decodedAuth = new String(Base64.decodeBase64(auth.substring(5).trim()), Charset.forName(UTF8));
            int pos = decodedAuth.indexOf(":");
            if (pos <= 0) {
                return null;
            }

            String login = decodedAuth.substring(0, pos);
            String password = decodedAuth.substring(pos + 1);

            try {
                return userService.authenticate(login, password);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }

    private AccessKey authKey(ContainerRequestContext requestContext){
        String auth = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.substring(0, 6).equalsIgnoreCase("Bearer")) {
            String key = auth.substring(6).trim();
            return accessKeyService.authenticate(key);
        }
        return null;
    }
}


