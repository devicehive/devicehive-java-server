package com.devicehive.exceptions.dao;

/**
 * @author Nikolay Loboda
 * @since 7/22/13 1:25 AM
 */
public class DublicateEntryException extends  HiveDataException {

    public DublicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }

    public DublicateEntryException(String message){
        super(message);
    }

}
