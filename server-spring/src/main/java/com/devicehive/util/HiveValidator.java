package com.devicehive.util;


import com.devicehive.exceptions.HiveException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.HashSet;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Component
public class HiveValidator {

    @Autowired
    private Validator validator;

    /**
     * Validates object representation.
     *
     * @param object Object that should be validated
     * @throws HiveException if there is any constraint violations.
     */
    public <T> void validate(T object) {
        Set<ConstraintViolation<?>> violations = new HashSet<ConstraintViolation<?>>(validator.validate(object));
        if (!violations.isEmpty()) {
            String response = buildMessage(violations);
            throw new HiveException(response, BAD_REQUEST.getStatusCode());
        }
    }

    public String buildMessage(Set<ConstraintViolation<?>> violations) {
        StringBuilder builder = new StringBuilder("Error! Validation failed: \n");
        for (ConstraintViolation<?> cv : violations) {
            builder.append(String.format("On property %s (value: %s): %s ; %n", cv.getPropertyPath(),
                                         cv.getInvalidValue(), cv.getMessage()));
        }
        return StringUtils.removeEnd(builder.toString(), " \n");
    }

}
