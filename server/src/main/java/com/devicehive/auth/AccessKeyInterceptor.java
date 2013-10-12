package com.devicehive.auth;

import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.UserStatus;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

@Interceptor
@AllowedKeyAction
@Priority(Interceptor.Priority.APPLICATION + 300)
public class AccessKeyInterceptor {

    private static Logger logger = LoggerFactory.getLogger(AccessKeyInterceptor.class);

    @AroundInvoke
    public Object checkPermissions(InvocationContext context) throws Exception {
        try {
            logger.debug(Thread.currentThread().getName());
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
            boolean isAllowed = CheckPermissionsHelper.checkAllPermissions(key, actions);
            if (!isAllowed) {
                throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
            }
            return context.proceed();
        } finally {
            ThreadLocalVariablesKeeper.clean();
        }
    }


}
