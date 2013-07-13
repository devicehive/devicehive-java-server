package com.devicehive.exceptions;


import javax.ejb.ApplicationException;

@ApplicationException
public class HiveException extends RuntimeException {

    public HiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveException(String message) {
        super(message);
    }

}
