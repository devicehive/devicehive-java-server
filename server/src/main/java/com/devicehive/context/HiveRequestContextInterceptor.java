package com.devicehive.context;



import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@HiveRequest
@Priority(Interceptor.Priority.APPLICATION - 500)
@Interceptor
public class HiveRequestContextInterceptor {

    @Inject
    private HiveContextExtension hiveContextExtension;

    @AroundInvoke
    public Object activateContext(InvocationContext invocationContext) throws Exception {
        HiveRequestContext hiveRequestContext = hiveContextExtension.getContext();
        try {
            hiveRequestContext.activate();
            return invocationContext.proceed();
        } finally {
            hiveRequestContext.deactivate();
        }
    }
}


