package com.devicehive.auth;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Aspect
@Component
public class RequestInterceptor {

    @Pointcut("execution(public * com.devicehive.controller.*.*(..))")
    public void publicMethod() {}

    @Around("publicMethod() && @annotation(com.devicehive.auth.AllowedKeyAction)")
    public Object checkPermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            HiveAuthentication hiveAuthentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
            HivePrincipal principal = (HivePrincipal) hiveAuthentication.getPrincipal();
            HiveAuthentication.HiveAuthDetails details = (HiveAuthentication.HiveAuthDetails) hiveAuthentication.getDetails();

            User user = principal.getUser();
            if (user != null && user.getStatus() != UserStatus.ACTIVE) {
                throw new HiveException(UNAUTHORIZED.getReasonPhrase(), UNAUTHORIZED.getStatusCode());
            }
            Device device = principal.getDevice();
            if (device != null && Boolean.TRUE.equals(device.getBlocked())) {
                throw new HiveException(String.format(Messages.DEVICE_IS_BLOCKED, device.getGuid()), FORBIDDEN.getStatusCode());
            }
            AccessKey key = principal.getKey();
            if (key == null) {
                return joinPoint.proceed();
            }
            if (key.getUser() == null || !key.getUser().getStatus().equals(UserStatus.ACTIVE)) {
                throw new HiveException(UNAUTHORIZED.getReasonPhrase(), UNAUTHORIZED.getStatusCode());
            }
            Timestamp expirationDate = key.getExpirationDate();
            if (expirationDate != null && expirationDate.before(new Timestamp(System.currentTimeMillis()))) {
                throw new HiveException(UNAUTHORIZED.getReasonPhrase(), UNAUTHORIZED.getStatusCode());
            }
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            Set<AccessKeyPermission>
                filtered =
                CheckPermissionsHelper.filterPermissions(key.getPermissions(), allowedActionAnnotation.action(),
                                                         details.getClientInetAddress(),
                                                         details.getOrigin());
            if (filtered.isEmpty()) {
                throw new HiveException(UNAUTHORIZED.getReasonPhrase(), UNAUTHORIZED.getStatusCode());
            }
            key.setPermissions(filtered);
            return joinPoint.proceed();
        } finally {
            ThreadLocalVariablesKeeper.clean();
        }
    }


}
