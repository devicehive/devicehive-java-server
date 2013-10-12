package com.devicehive.auth.websockets;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.service.AccessKeyService;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.handlers.annotations.WebsocketController;
import com.devicehive.websockets.util.WebsocketSession;

import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;

@Interceptor
@WebsocketController
@Priority(Interceptor.Priority.APPLICATION + 200)
public class AuthorizationInterceptor {

    @EJB
    private AccessKeyService accessKeyService;


    @AroundInvoke
    public Object authorize(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        boolean allowed = false;
        try {
            HivePrincipal principal = deployHivePrincipal();
            if (method.isAnnotationPresent(RolesAllowed.class)) {
                RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
                String[] roles = rolesAllowed.value();
                for (String role : roles) {
                    switch (role) {
                        case HiveRoles.ADMIN:
                            allowed = allowed || (principal != null && principal.getUser() != null && principal.getUser().isAdmin());
                            break;
                        case HiveRoles.CLIENT:
                            allowed = allowed || (principal != null && principal.getUser() != null);
                            break;
                        case HiveRoles.DEVICE:
                            allowed = allowed || (principal != null && principal.getDevice() != null);
                            break;
                        case HiveRoles.KEY:
                            allowed = allowed || (principal != null && principal.getKey() != null);
                            break;
                        default:
                    }
                }
            } else {
                allowed = method.isAnnotationPresent(PermitAll.class);
            }
            if (!allowed) {
                throw new HiveException("Forbidden", Response.Status.FORBIDDEN.getStatusCode());
            }
            return context.proceed();
        } finally {
            ThreadLocalVariablesKeeper.setPrincipal(null);
        }
    }


    private HivePrincipal deployHivePrincipal() {
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (principal == null) {
            HivePrincipal sessionPrincipal = WebsocketSession.getPrincipal(ThreadLocalVariablesKeeper.getSession());
            if (sessionPrincipal != null) {
                principal = new HivePrincipal(
                        sessionPrincipal.getUser(),
                        sessionPrincipal.getDevice(),
                        sessionPrincipal.getKey() != null
                                ? accessKeyService.authenticate(sessionPrincipal.getKey().getKey())
                                : null

                );
                WebsocketSession.setPrincipal(ThreadLocalVariablesKeeper.getSession(), principal);
            }
            ThreadLocalVariablesKeeper.setPrincipal(principal);
        }
        return principal;
    }
}
