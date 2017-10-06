package com.devicehive.util;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


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

    private final Validator validator;

    @Autowired
    public HiveValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates object representation.
     *
     * @param object Object that should be validated
     * @throws HiveException if there is any constraint violations.
     */
    public <T> void validate(T object) {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        try {
            violations.addAll(validator.validate(object));
        } catch (IllegalArgumentException e){
            throw new HiveException("Error! Validation failed: Object is null", BAD_REQUEST.getStatusCode());
        }
        if (!violations.isEmpty()) {
            String response = buildMessage(violations);
            throw new HiveException(response, BAD_REQUEST.getStatusCode());
        }
    }

    private String buildMessage(Set<ConstraintViolation<?>> violations) {
        StringBuilder builder = new StringBuilder("Error! Make sure, that the passed object is correct and properly structured. Validation failed: \n");
        for (ConstraintViolation<?> cv : violations) {
            builder.append(String.format("On property %s (value: %s): %s ; %n", cv.getPropertyPath(),
                                         cv.getInvalidValue(), cv.getMessage()));
        }
        return StringUtils.removeEnd(builder.toString(), String.format("%n"));
    }

}
