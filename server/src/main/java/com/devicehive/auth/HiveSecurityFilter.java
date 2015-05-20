package com.devicehive.auth;

import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKey;
import com.devicehive.model.Device;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.OAuthClientService;
import com.devicehive.service.UserService;
import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;

/**
 * Created by stas on 03.08.14.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class HiveSecurityFilter implements ContainerRequestFilter {

    @Inject
    private DeviceService deviceService;
    @Inject
    private UserService userService;
    @Inject
    private AccessKeyService accessKeyService;
    @Inject
    private OAuthClientService clientService;
    @Inject
    private HiveSecurityContext hiveSecurityContext;
    @Context
    private HttpServletRequest servletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        HivePrincipal principal = new HivePrincipal(
                authUser(requestContext),
                authDevice(requestContext),
                authKey(requestContext));
        hiveSecurityContext.setHivePrincipal(principal);
        hiveSecurityContext.setoAuthClient(authClient(requestContext));
        hiveSecurityContext.setClientInetAddress(InetAddress.getByName(servletRequest.getRemoteAddr()));
        hiveSecurityContext.setOrigin(requestContext.getHeaderString(com.google.common.net.HttpHeaders.ORIGIN));
        hiveSecurityContext.setAuthorization(requestContext.getHeaderString(com.google.common.net.HttpHeaders.AUTHORIZATION));
        hiveSecurityContext.setSecure(requestContext.getSecurityContext().isSecure());
        requestContext.setSecurityContext(hiveSecurityContext);
    }

    private Device authDevice(ContainerRequestContext requestContext) throws IOException {
        String deviceId = requestContext.getHeaderString(Constants.AUTH_DEVICE_ID_HEADER);
        if (deviceId == null) {
            return null;
        }
        String deviceKey = requestContext.getHeaderString(Constants.AUTH_DEVICE_KEY_HEADER);
        return deviceService.authenticate(deviceId, deviceKey);

    }

    private User authUser(ContainerRequestContext requestContext) throws IOException {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.substring(0, 5).equalsIgnoreCase(Constants.BASIC_AUTH_SCHEME)) {
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

    private AccessKey authKey(ContainerRequestContext requestContext) throws IOException {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.substring(0, 6).equalsIgnoreCase(Constants.OAUTH_AUTH_SCEME)) {
            String key = auth.substring(6).trim();
            return accessKeyService.authenticate(key);
        }
        return null;
    }

    private OAuthClient authClient(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.substring(0, 5).equalsIgnoreCase(Constants.BASIC_AUTH_SCHEME)) {
            String decodedAuth = new String(Base64.decodeBase64(auth.substring(5).trim()), Charsets.UTF_8);
            int pos = decodedAuth.indexOf(":");
            if (pos <= 0) {
                return null;
            }

            String openAuthID = decodedAuth.substring(0, pos);
            String openAuthSecret = decodedAuth.substring(pos + 1);

            try {
                return clientService.authenticate(openAuthID, openAuthSecret);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }
}
