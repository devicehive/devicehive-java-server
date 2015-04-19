package com.devicehive.handler;

import com.devicehive.domain.error.ErrorResponse;
import com.devicehive.exception.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by tmatvienko on 4/19/15.
 */
@ControllerAdvice
public class DefaultExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody ErrorResponse defaultErrorHandler(HttpServletRequest req, HttpServletResponse res, Exception e) {
        LOGGER.error("Exception occured: ", e);
        res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(value = HiveException.class)
    @ResponseBody ErrorResponse devicehiveErrorHandler(HttpServletRequest req, HttpServletResponse res, HiveException e) {
        LOGGER.error("DeviceHiveException occured: {}", e);
        res.setStatus(e.getStatus().value());
        return new ErrorResponse(e.getStatus(), e.getMessage());
    }
}
