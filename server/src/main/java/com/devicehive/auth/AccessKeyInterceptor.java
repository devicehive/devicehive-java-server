package com.devicehive.auth;

import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.UserStatus;
import com.devicehive.utils.ThreadLocalVariablesKeeper;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Interceptor
@AllowedKeyAction
@Priority(Interceptor.Priority.APPLICATION + 300)
public class AccessKeyInterceptor {

    @AroundInvoke
    public Object checkPermissions(InvocationContext context) throws Exception {
        try {
            HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
            AccessKey key = principal.getKey();
            if (key == null) {
                return context.proceed();
            }
            if (key.getUser() == null || !key.getUser().getStatus().equals(UserStatus.ACTIVE)) {
                throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
            }
            Timestamp expirationDate = key.getExpirationDate();
            if (expirationDate != null && expirationDate.before(new Timestamp(System.currentTimeMillis()))){
                throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
            }
            Method method = context.getMethod();
            AllowedKeyAction allowedActionAnnotation = method.getAnnotation(AllowedKeyAction.class);
            List<AllowedKeyAction.Action> actions = Arrays.asList(allowedActionAnnotation.action());
            InetAddress clientIP = ThreadLocalVariablesKeeper.getClientIP();

            Set<AccessKeyPermission> permissions = key.getPermissions();
            boolean isAllowed = CheckPermissionsHelper.checkActions(actions, permissions)
                    && CheckPermissionsHelper.checkIP(clientIP, permissions)
                    && CheckPermissionsHelper.checkDeviceGuids(permissions)
                    && CheckPermissionsHelper.checkNetworks(permissions)
                    && CheckPermissionsHelper.checkDomains(permissions);
            if (!isAllowed) {
                throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
            }
            return context.proceed();
        } finally {
            ThreadLocalVariablesKeeper.clean();
        }
    }


}
