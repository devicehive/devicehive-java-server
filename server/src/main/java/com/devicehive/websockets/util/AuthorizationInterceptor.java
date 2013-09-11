package com.devicehive.websockets.util;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.websockets.handlers.annotations.Authorize;

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
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
            HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
            User authUser = principal.getUser();
            Device authDevice = principal.getDevice();
            String[] roles = rolesAllowed.value();
            boolean allowed = false;
            for (String role : roles){
                switch (role){
                    case HiveRoles.ADMIN :
                        allowed = allowed || authUser.isAdmin();
                        break;
                    case HiveRoles.CLIENT :
                        allowed = allowed || !authUser.isAdmin();
                        break;
                    case HiveRoles.DEVICE :
                        allowed = allowed || authDevice != null;
                }
            }
            if (!allowed){
                throw new HiveException("Not authorised", Response.Status.UNAUTHORIZED.getStatusCode());
            }
            return context.proceed();
        } else if (method.isAnnotationPresent(PermitAll.class)) {
            return context.proceed();
        }
        throw new HiveException("Forbidden", Response.Status.FORBIDDEN.getStatusCode());
    }
}
