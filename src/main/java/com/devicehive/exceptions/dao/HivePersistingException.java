package com.devicehive.exceptions.dao;

/**
 * @author Nikolay Loboda
 * @since 7/21/13 12:41 AM
 */
public class HivePersistingException extends HiveDataException {

    public HivePersistingException(String message, Throwable cause) {
        super(message, cause);
    }

    public HivePersistingException(String message) {
        super(message);
    }
}