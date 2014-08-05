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
import com.sun.org.apache.xml.internal.utils.ThreadControllerWrapper;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;

/**
 * Created by stas on 03.08.14.
 */
@WebFilter(value = "/*", asyncSupported = true)
public class HiveSecurityFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(HiveSecurityFilter.class);

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


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        hiveSecurityContext.setHivePrincipal(new HivePrincipal(authUser(httpServletRequest), authDevice(httpServletRequest),
                authKey(httpServletRequest)));
        hiveSecurityContext.setoAuthClient(authClient(httpServletRequest));
        hiveSecurityContext.setClientInetAddress(InetAddress.getByName(request.getRemoteAddr()));
        hiveSecurityContext.setOrigin(httpServletRequest.getHeader(com.google.common.net.HttpHeaders.ORIGIN));
        hiveSecurityContext.setAuthorization(httpServletRequest.getHeader(com.google.common.net.HttpHeaders.AUTHORIZATION));
        httpServletRequest.setAttribute(HiveSecurityContext.class.getName(), hiveSecurityContext);
        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }



    private Device authDevice(HttpServletRequest request) throws IOException {
        String deviceId = request.getHeader(Constants.AUTH_DEVICE_ID_HEADER);
        if (deviceId == null) {
            return null;
        }
        String deviceKey = request.getHeader(Constants.AUTH_DEVICE_KEY_HEADER);
        return deviceService.authenticate(deviceId, deviceKey);

    }

    private User authUser(HttpServletRequest request) throws IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
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

    private AccessKey authKey(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.substring(0, 6).equalsIgnoreCase(Constants.OAUTH_AUTH_SCEME)) {
            String key = auth.substring(6).trim();
            return accessKeyService.authenticate(key);
        }
        return null;
    }

    private OAuthClient authClient(HttpServletRequest request) {
        String auth =  request.getHeader(HttpHeaders.AUTHORIZATION);
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
