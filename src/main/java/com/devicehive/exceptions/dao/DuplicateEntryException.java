package com.devicehive.exceptions.dao;

/**
 * @author Nikolay Loboda
 * @since 7/22/13 1:25 AM
 */
public class DuplicateEntryException extends  HiveDataException {

    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateEntryException(String message){
        super(message);
    }

}
