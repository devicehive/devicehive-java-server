package com.devicehive.service.interceptors;


import com.devicehive.exceptions.HiveException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class JsonInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(JsonInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } catch (JsonSyntaxException e) {
            throw new HiveException("Incorrect JSON syntax: " + e.getCause().getLocalizedMessage(), e);
        } catch (JsonParseException e) {
            throw new HiveException("Error occurred on parsing JSON object: " + e.getLocalizedMessage(), e);
        }
    }

}
