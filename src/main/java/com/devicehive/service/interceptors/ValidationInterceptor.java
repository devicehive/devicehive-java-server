package com.devicehive.service.interceptors;


import com.devicehive.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

public class ValidationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ValidationInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception{
        try {
            return  ctx.proceed();
        } catch (ConstraintViolationException ex) {
            logger.debug("Validation error, incorrect input");
            throw new HiveException(ex.getMessage()); //TODO create message
        }
    }

}
