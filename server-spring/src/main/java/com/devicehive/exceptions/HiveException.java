package com.devicehive.exceptions;


import com.devicehive.configuration.Messages;

import javax.servlet.http.HttpServletResponse;

public class HiveException extends RuntimeException {

    private static final long serialVersionUID = 6413354755792688308L;

    private Integer code = null;

    public Integer getCode() {
        return code;
    }

    public HiveException(String message, Throwable cause) {
        this(message, cause, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message) {
        this(message, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message, Integer code) {
        this(message, null, code);
    }

    public HiveException(String message, Throwable cause, Integer code) {
        super(message, cause);
        this.code = code != null ? code : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    public static HiveException fatal() {
        return new HiveException(Messages.INTERNAL_SERVER_ERROR);
    }

}
