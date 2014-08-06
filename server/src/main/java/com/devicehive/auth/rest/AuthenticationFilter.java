package com.devicehive.auth.rest;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.model.*;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.OAuthClientService;
import com.devicehive.service.UserService;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;

import static com.devicehive.configuration.Constants.UTF8;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        HiveSecurityContext hiveSecurityContext = (HiveSecurityContext) requestContext.getProperty(HiveSecurityContext.class.getName());
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        requestContext.setSecurityContext(new SecurityContext() {
            private HivePrincipal hivePrincipal = principal;
            private boolean secure = requestContext.getSecurityContext().isSecure();

            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String roleString) {
                switch (roleString) {
                    case HiveRoles.DEVICE:
                        return hivePrincipal != null && hivePrincipal.getDevice() != null;
                    case HiveRoles.KEY:
                        return hivePrincipal != null && hivePrincipal.getKey() != null;
                    default:
                        return hivePrincipal != null
                                && hivePrincipal.getUser() != null
                                && hivePrincipal.getUser().getRole() == UserRole.valueOf(roleString);
                }
            }

            @Override
            public boolean isSecure() {
                return secure;
            }

            @Override
            public String getAuthenticationScheme() {
                if (hivePrincipal.getKey() != null) {
                    return Constants.OAUTH_AUTH_SCEME;
                }
                return BASIC_AUTH;
            }
        });
    }


}


