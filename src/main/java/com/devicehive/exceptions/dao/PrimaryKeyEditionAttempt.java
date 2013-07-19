package com.devicehive.exceptions.dao;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
public class PrimaryKeyEditionAttempt extends HiveDataException {

    public PrimaryKeyEditionAttempt(String message, Throwable cause) {
        super(message, cause);
    }

    public PrimaryKeyEditionAttempt(String message){
        super(message);
    }
}
