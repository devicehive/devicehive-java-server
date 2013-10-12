package com.devicehive.util;


import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

@LogExecutionTime
@Interceptor
public class ExecutionTimeLoggerInterceptor {

    @AroundInvoke
    public Object log(InvocationContext ctx) throws Exception {
        Method method = ctx.getMethod();
        String name = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        Timer t = Timer.newInstance();
        try {
            return ctx.proceed();
        } finally {
            t.logMethodExecuted(name);
        }
    }
}
