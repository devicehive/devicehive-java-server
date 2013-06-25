package com.devicehive.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 24.06.13
 * Time: 20:28
 */
public class HiveException extends RuntimeException {

    public HiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveException(String message) {
        super(message);
    }

}
