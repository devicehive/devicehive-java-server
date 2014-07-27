package com.devicehive.context;



import javax.annotation.Priority;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@HiveRequest
@Priority(Interceptor.Priority.APPLICATION - 500)
public class HiveRequestContextInterceptor {

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object activateContext(InvocationContext invocationContext) throws Exception {
        HiveRequestContext hiveRequestContext = (HiveRequestContext) beanManager.getContext(HiveRequestScoped.class);
        try {
            hiveRequestContext.activate();
            return invocationContext.proceed();
        } finally {
            hiveRequestContext.deactivate();
        }
    }
}


