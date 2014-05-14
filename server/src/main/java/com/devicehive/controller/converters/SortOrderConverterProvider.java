package com.devicehive.controller.converters;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Provider
public class SortOrderConverterProvider implements ParamConverterProvider {

    private static final Logger logger = LoggerFactory.getLogger(ParamConverterProvider.class);

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            logger.debug("annotation : {}", annotation);
            if (annotation.annotationType().equals(SortOrder.class) && Boolean.class.equals(rawType)) {
                ParamConverter<T> converter = (ParamConverter<T>) new SortOrderParametersConverter();
                return converter;
            }
        }
        return null;
    }

    public static class SortOrderParametersConverter implements ParamConverter<Boolean> {
        private final static String SORT_ORDER_ASC = "ASC";
        private final static String SORT_ORDER_DESC = "DESC";

        @Override
        public Boolean fromString(String value) {
            if (value == null || value.equalsIgnoreCase(SORT_ORDER_ASC)) {
                return true;
            } else if (value.equalsIgnoreCase(SORT_ORDER_DESC)) {
                return false;
            } else {
                throw new HiveException(String.format(Messages.UNPARSEABLE_SORT_ORDER, value),
                        BAD_REQUEST.getStatusCode());
            }
        }

        @Override
        public String toString(Boolean value) {
            if (value == null) {
                throw new HiveException(Messages.SORT_ORDER_IS_NULL, INTERNAL_SERVER_ERROR.getStatusCode());
            } else if (value) {
                return SORT_ORDER_ASC;
            } else {
                return SORT_ORDER_DESC;
            }
        }
    }
}
