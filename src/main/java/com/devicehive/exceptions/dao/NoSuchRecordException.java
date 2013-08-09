package com.devicehive.exceptions.dao;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
public class NoSuchRecordException extends HiveDataException {

    public NoSuchRecordException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchRecordException(String message) {
        super(message);
    }
}
