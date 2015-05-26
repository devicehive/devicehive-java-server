package com.devicehive.auth.websockets;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.exceptions.HiveException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;

@Aspect
@Component
@Order(1)
public class AuthorizationAspect {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationAspect.class);

    @Before("execution(public * com.devicehive.websockets.handlers.WebsocketHandlers+.*(..))")
    public void authorize(JoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        boolean allowed = false;
//        HivePrincipal principal = hiveAuthentication.getHivePrincipal();
        if (method.isAnnotationPresent(RolesAllowed.class)) {
//            RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
//            String[] roles = rolesAllowed.value();
//            for (String role : roles) {
//                switch (role) {
//                    case HiveRoles.ADMIN:
//                        allowed = allowed ||
//                                (principal != null && principal.getUser() != null && principal.getUser().isAdmin());
//                        break;
//                    case HiveRoles.CLIENT:
//                        allowed = allowed || (principal != null && principal.getUser() != null);
//                        break;
//                    case HiveRoles.DEVICE:
//                        allowed = allowed || (principal != null && principal.getDevice() != null);
//                        break;
//                    case HiveRoles.KEY:
//                        allowed = allowed || (principal != null && principal.getKey() != null);
//                        break;
//                    default:
//                }
//            }
        } else {
            allowed = method.isAnnotationPresent(PermitAll.class);
        }
        if (!allowed) {
//            if (principal != null) {
//                logger.error("Authorization failed: user {}, device {}, accessKey {}", principal.getUser(), principal.getDevice(), principal.getKey());
//            } else {
//                logger.error("Authorization failed: principal is null");
//            }
            throw new HiveException(Response.Status.FORBIDDEN.getReasonPhrase(),
                    Response.Status.FORBIDDEN.getStatusCode());
        }
    }
}
