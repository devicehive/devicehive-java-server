package com.devicehive.websockets.util;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.utils.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.handlers.annotations.Authorize;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;

@Interceptor
@Authorize
@Priority(Interceptor.Priority.APPLICATION + 200)
public class AuthorizationInterceptor {


    @AroundInvoke
    public Object authorize(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        boolean allowed = false;
        try {
            if (method.isAnnotationPresent(RolesAllowed.class)) {
                RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
                HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
                if (principal == null) {
                    principal = WebsocketSession.getPrincipal(ThreadLocalVariablesKeeper.getSession());
                    //TODO fixme it is needed to make a clone of access key permissions set
                    // TODO otherwise it becomes dirty after first use
                    ThreadLocalVariablesKeeper.setPrincipal(principal);
                }
                User authUser = principal != null ? principal.getUser() : null;
                Device authDevice = principal != null ? principal.getDevice() : null;
                AccessKey accessKey = principal != null ? principal.getKey() : null;
                String[] roles = rolesAllowed.value();
                for (String role : roles) {
                    switch (role) {
                        case HiveRoles.ADMIN:
                            allowed = allowed || (authUser != null && authUser.isAdmin());
                            break;
                        case HiveRoles.CLIENT:
                            allowed = allowed || authUser != null;
                            break;
                        case HiveRoles.DEVICE:
                            allowed = allowed || authDevice != null;
                            break;
                        case HiveRoles.KEY:
                            allowed = allowed || accessKey != null;
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
}
