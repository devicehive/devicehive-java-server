package com.devicehive.exceptions.dao;

import com.devicehive.exceptions.HiveException;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
public class HiveDataException extends HiveException {

    public HiveDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveDataException(String message){
        super(message);
    }
}
