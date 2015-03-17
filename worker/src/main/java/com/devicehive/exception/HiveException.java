package com.devicehive.exception;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Created by tatyana on 2/9/15.
 */
public class HiveException extends RuntimeException {

    private Integer code = null;

    public HiveException(String message, Throwable cause) {
        this(message, cause, SC_INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message) {
        this(message, null, SC_INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message, int code) {
        this(message, null, code);
    }

    public HiveException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
