package com.devicehive.service.interceptors;


import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

public class ValidationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ValidationInterceptor.class);
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        try {
            Object proccedResult = ctx.proceed();
            if (em.isJoinedToTransaction()) {
                em.flush();
            }
            return proccedResult;
        } catch (ConstraintViolationException ex) {
            logger.debug("[processMessage] Validation error, incorrect input");
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            StringBuilder builderForResponse = new StringBuilder("Validation failed: \n");
            for (ConstraintViolation<?> constraintViolation : constraintViolations) {
                builderForResponse.append(constraintViolation.getMessage());
                builderForResponse.append("\n");
            }
            throw new HiveException(builderForResponse.toString());
        }
    }

}
